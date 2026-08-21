package com.generalisthealthai.rcm.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.generalisthealthai.rcm.api.dto.AuditJobStatusResponseDto;
import com.generalisthealthai.rcm.api.dto.AuditReportResponseDto;
import com.generalisthealthai.rcm.domain.enums.AuditType;
import com.generalisthealthai.rcm.domain.enums.DenialRisk;
import com.generalisthealthai.rcm.domain.enums.JobStatus;
import com.generalisthealthai.rcm.domain.enums.ReviewDecision;
import com.generalisthealthai.rcm.domain.model.AuditJob;
import com.generalisthealthai.rcm.domain.model.ClaimMetadata;
import com.generalisthealthai.rcm.infrastructure.persistence.AuditJobRepository;
import com.generalisthealthai.rcm.ingestion.dto.ClaimIngestRequestDto;
import com.generalisthealthai.rcm.ingestion.ingestor.ClaimMetadataIngestor;
import com.generalisthealthai.rcm.medprompt.ensemble.EnsembleOrchestrator;
import com.generalisthealthai.rcm.medprompt.ensemble.dto.EnsembleConsensusResult;
import com.generalisthealthai.rcm.medprompt.llm.LlmRouter;
import com.generalisthealthai.rcm.medprompt.retrieval.VectorSearchService;
import com.generalisthealthai.rcm.output.AppealLetterGenerator;
import com.generalisthealthai.rcm.output.AuditReportGenerator;
import com.generalisthealthai.rcm.service.AuditJobService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuditJobController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuditJobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuditJobService auditJobService;

    @MockBean
    private AuditJobRepository auditJobRepository;

    @MockBean
    private ClaimMetadataIngestor claimIngestor;

    @MockBean
    private VectorSearchService vectorSearchService;

    @MockBean
    private EnsembleOrchestrator ensembleOrchestrator;

    @MockBean
    private LlmRouter llmRouter;

    @MockBean
    private AuditReportGenerator reportGenerator;

    @MockBean
    private AppealLetterGenerator appealLetterGenerator;

    @Test
    void testSubmitClaimAuditAsync() throws Exception {
        ClaimIngestRequestDto requestDto = ClaimIngestRequestDto.builder()
                .payerId("BCBS")
                .cptCodes(List.of("93000"))
                .primaryIcd10("R07.9")
                .build();

        UUID jobId = UUID.randomUUID();
        AuditJob mockJob = AuditJob.builder()
                .id(jobId)
                .status(JobStatus.PENDING)
                .auditType(AuditType.CLAIM_DENIAL_PREDICTION)
                .payerId("BCBS")
                .createdAt(Instant.now())
                .build();

        when(auditJobService.submitClaimAuditJob(any(ClaimIngestRequestDto.class))).thenReturn(mockJob);

        mockMvc.perform(post("/api/v1/rcm/audit/claim")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").value(jobId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.payerId").value("BCBS"));
    }

    @Test
    void testGetJobStatus() throws Exception {
        UUID jobId = UUID.randomUUID();
        AuditJobStatusResponseDto responseDto = AuditJobStatusResponseDto.builder()
                .jobId(jobId)
                .status(JobStatus.DONE)
                .payerId("BCBS")
                .report(AuditReportResponseDto.builder()
                        .decision(ReviewDecision.PAID)
                        .denialRisk(DenialRisk.LOW_RISK)
                        .riskScore(0.05)
                        .build())
                .build();

        when(auditJobService.getJobStatus(jobId)).thenReturn(Optional.of(responseDto));

        mockMvc.perform(get("/api/v1/rcm/jobs/" + jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(jobId.toString()))
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.report.decision").value("PAID"));
    }

    @Test
    void testGetJobStatusNotFound() throws Exception {
        UUID randomId = UUID.randomUUID();
        when(auditJobService.getJobStatus(randomId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/rcm/jobs/" + randomId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }
}
