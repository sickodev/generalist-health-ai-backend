package com.generalisthealthai.rcm.e2e;

import com.generalisthealthai.rcm.api.dto.AppealDraftRequestDto;
import com.generalisthealthai.rcm.api.dto.AuditJobStatusResponseDto;
import com.generalisthealthai.rcm.api.dto.AuditReportResponseDto;
import com.generalisthealthai.rcm.api.dto.ExemplarResponseDto;
import com.generalisthealthai.rcm.domain.enums.JobStatus;
import com.generalisthealthai.rcm.domain.enums.ReviewDecision;
import com.generalisthealthai.rcm.ingestion.dto.ClaimIngestRequestDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RcmPipelineE2EIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testEndToEndSyncClaimAudit() {
        ClaimIngestRequestDto requestDto = ClaimIngestRequestDto.builder()
                .claimId("CLM-E2E-001")
                .payerId("BCBS")
                .patientId("PAT-E2E-001")
                .cptCodes(List.of("93000"))
                .primaryIcd10("R07.9")
                .billedAmount(new BigDecimal("350.00"))
                .placeOfService("11")
                .dateOfService("2026-08-21")
                .build();

        ResponseEntity<AuditReportResponseDto> response = restTemplate.postForEntity(
                "/api/v1/rcm/audit/claim/sync",
                requestDto,
                AuditReportResponseDto.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        AuditReportResponseDto report = response.getBody();
        assertNotNull(report);
        assertEquals(ReviewDecision.PAID, report.getDecision());
        assertNotNull(report.getAuditRationale());
        assertTrue(report.getRiskScore() < 0.30);
    }

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Test
    void testEndToEndAsyncClaimAuditSubmissionAndPolling() throws Exception {
        ClaimIngestRequestDto requestDto = ClaimIngestRequestDto.builder()
                .claimId("CLM-E2E-002")
                .payerId("UnitedHealthcare")
                .patientId("PAT-E2E-002")
                .cptCodes(List.of("73721"))
                .primaryIcd10("M25.561")
                .billedAmount(new BigDecimal("1450.00"))
                .placeOfService("11")
                .dateOfService("2026-08-21")
                .build();

        ResponseEntity<String> submitResponse = restTemplate.postForEntity(
                "/api/v1/rcm/audit/claim",
                requestDto,
                String.class
        );

        assertEquals(HttpStatus.ACCEPTED, submitResponse.getStatusCode());
        assertNotNull(submitResponse.getBody());
        AuditJobStatusResponseDto submitDto = objectMapper.readValue(submitResponse.getBody(), AuditJobStatusResponseDto.class);
        var jobId = submitDto.getJobId();
        assertNotNull(jobId);

        // Poll for completion (up to 5 seconds)
        AuditJobStatusResponseDto finalStatus = null;
        for (int i = 0; i < 10; i++) {
            Thread.sleep(500);
            ResponseEntity<String> pollResponse = restTemplate.getForEntity(
                    "/api/v1/rcm/jobs/" + jobId,
                    String.class
            );
            if (pollResponse.getStatusCode() == HttpStatus.OK && pollResponse.getBody() != null) {
                AuditJobStatusResponseDto pollDto = objectMapper.readValue(pollResponse.getBody(), AuditJobStatusResponseDto.class);
                if (pollDto.getStatus() == JobStatus.DONE) {
                    finalStatus = pollDto;
                    break;
                }
            }
        }

        assertNotNull(finalStatus, "Job should complete within 5 seconds");
        assertEquals(JobStatus.DONE, finalStatus.getStatus());
        assertNotNull(finalStatus.getReport());
        assertEquals(ReviewDecision.DENIED, finalStatus.getReport().getDecision());
        assertTrue(finalStatus.getReport().getPredictedDenialCodes().contains("CO-197"));
    }

    @Test
    void testEndToEndExemplarsListing() {
        ResponseEntity<List<ExemplarResponseDto>> response = restTemplate.exchange(
                "/api/v1/rcm/exemplars",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<ExemplarResponseDto> exemplars = response.getBody();
        assertNotNull(exemplars);
        assertTrue(exemplars.size() >= 6, "Seed exemplars should be pre-populated on startup");
    }

    @Test
    void testEndToEndAppealDrafting() {
        AppealDraftRequestDto appealRequest = AppealDraftRequestDto.builder()
                .claim(ClaimIngestRequestDto.builder()
                        .claimId("CLM-APPEAL-01")
                        .payerId("Aetna")
                        .patientId("PAT-001")
                        .cptCodes(List.of("97110"))
                        .primaryIcd10("M54.5")
                        .build())
                .denialCodes(List.of("CO-16"))
                .auditRationale("Patient requires extended physical therapy for severe lumbar disc injury.")
                .customPayerPolicy("Aetna CPB #0250 Physical Therapy Guidelines")
                .build();

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/rcm/appeal/draft",
                appealRequest,
                Map.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        String letter = (String) response.getBody().get("appealLetter");
        assertNotNull(letter);
        assertTrue(letter.contains("Aetna"));
        assertTrue(letter.contains("CO-16"));
        assertTrue(letter.contains("CPB #0250"));
    }
}
