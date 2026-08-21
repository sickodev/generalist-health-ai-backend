package com.generalisthealthai.rcm.output;

import com.generalisthealthai.rcm.domain.enums.DenialRisk;
import com.generalisthealthai.rcm.domain.enums.ReviewDecision;
import com.generalisthealthai.rcm.domain.model.AuditReport;
import com.generalisthealthai.rcm.domain.model.ClaimMetadata;
import com.generalisthealthai.rcm.medprompt.ensemble.dto.EnsembleConsensusResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AuditReportGeneratorTest {

    private AuditReportGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new AuditReportGenerator(new AppealLetterGenerator());
    }

    @Test
    void testGenerateReportFromConsensus() {
        UUID jobId = UUID.randomUUID();
        ClaimMetadata claim = ClaimMetadata.builder()
                .payerId("BCBS")
                .cptCodes(List.of("93000"))
                .primaryIcd10("R07.9")
                .build();

        EnsembleConsensusResult consensus = EnsembleConsensusResult.builder()
                .consensusDecision(ReviewDecision.PAID)
                .consensusDenialRisk(DenialRisk.LOW_RISK)
                .averageRiskScore(0.05)
                .consensusDenialCodes(List.of())
                .primaryRationale("EKG is medically indicated for acute chest pain.")
                .agreementConfidenceScore(1.0)
                .averagePatientResponsibility(new BigDecimal("20.00"))
                .build();

        AuditReport report = generator.generateReport(jobId, claim, consensus);

        assertNotNull(report);
        assertEquals(jobId, report.getJobId());
        assertEquals(ReviewDecision.PAID, report.getDecision());
        assertEquals(DenialRisk.LOW_RISK, report.getDenialRisk());
        assertEquals(0.05, report.getRiskScore());
        assertEquals("EKG is medically indicated for acute chest pain.", report.getAuditRationale());
        assertEquals(new BigDecimal("20.00"), report.getPatientResponsibilityAmount());
    }
}
