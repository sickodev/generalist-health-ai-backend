package com.generalisthealthai.rcm.medprompt.ensemble;

import com.generalisthealthai.rcm.domain.enums.DenialRisk;
import com.generalisthealthai.rcm.domain.enums.ReviewDecision;
import com.generalisthealthai.rcm.medprompt.ensemble.dto.EnsembleConsensusResult;
import com.generalisthealthai.rcm.medprompt.ensemble.dto.LlmAuditResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsensusVoterTest {

    private ConsensusVoter voter;

    @BeforeEach
    void setUp() {
        voter = new ConsensusVoter();
    }

    @Test
    void testMajorityVotingDeniedDecision() {
        LlmAuditResponseDto run1 = LlmAuditResponseDto.builder()
                .decision(ReviewDecision.DENIED)
                .denialRisk(DenialRisk.HIGH_RISK)
                .riskScore(0.85)
                .predictedDenialCodes(List.of("CO-50"))
                .stepByStepRationale("Medical necessity not established.")
                .appealLetterDraft("To BCBS Appeals Dept...")
                .patientResponsibility(new BigDecimal("100.00"))
                .build();

        LlmAuditResponseDto run2 = LlmAuditResponseDto.builder()
                .decision(ReviewDecision.DENIED)
                .denialRisk(DenialRisk.HIGH_RISK)
                .riskScore(0.90)
                .predictedDenialCodes(List.of("CO-50", "CO-16"))
                .stepByStepRationale("Diagnosis does not support procedure.")
                .appealLetterDraft("To BCBS Appeals Dept...")
                .patientResponsibility(new BigDecimal("100.00"))
                .build();

        LlmAuditResponseDto run3 = LlmAuditResponseDto.builder()
                .decision(ReviewDecision.DENIED)
                .denialRisk(DenialRisk.HIGH_RISK)
                .riskScore(0.80)
                .predictedDenialCodes(List.of("CO-50"))
                .stepByStepRationale("Denial expected under policy #104.")
                .patientResponsibility(new BigDecimal("100.00"))
                .build();

        LlmAuditResponseDto run4 = LlmAuditResponseDto.builder()
                .decision(ReviewDecision.PAID)
                .denialRisk(DenialRisk.LOW_RISK)
                .riskScore(0.15)
                .stepByStepRationale("Claim seems fine.")
                .build();

        EnsembleConsensusResult result = voter.vote(List.of(run1, run2, run3, run4));

        assertNotNull(result);
        assertEquals(ReviewDecision.DENIED, result.getConsensusDecision());
        assertEquals(DenialRisk.HIGH_RISK, result.getConsensusDenialRisk());
        assertEquals(0.75, result.getAgreementConfidenceScore()); // 3 out of 4 runs
        assertEquals(4, result.getTotalEnsembleRuns());
        assertEquals(3, result.getAgreeingRuns());
        assertTrue(result.getConsensusDenialCodes().contains("CO-50"));
        assertTrue(result.getConsensusDenialCodes().contains("CO-16"));
        assertNotNull(result.getPrimaryRationale());
        assertNotNull(result.getAppealLetterDraft());
    }

    @Test
    void testEmptyRunsReturnsSafeDefault() {
        EnsembleConsensusResult result = voter.vote(List.of());
        assertNotNull(result);
        assertEquals(ReviewDecision.UNKNOWN, result.getConsensusDecision());
        assertEquals(0.0, result.getAgreementConfidenceScore());
    }
}
