package com.generalisthealthai.rcm.medprompt.ensemble;

import com.generalisthealthai.rcm.domain.enums.DenialRisk;
import com.generalisthealthai.rcm.domain.enums.ReviewDecision;
import com.generalisthealthai.rcm.domain.model.ClaimMetadata;
import com.generalisthealthai.rcm.domain.model.RcmExemplar;
import com.generalisthealthai.rcm.medprompt.ensemble.dto.EnsembleConsensusResult;
import com.generalisthealthai.rcm.medprompt.ensemble.dto.LlmAuditResponseDto;
import com.generalisthealthai.rcm.medprompt.prompt.PromptBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EnsembleOrchestratorTest {

    private EnsembleOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        InputShuffler shuffler = new InputShuffler(new PromptBuilder());
        ConsensusVoter voter = new ConsensusVoter();
        orchestrator = new EnsembleOrchestrator(
                shuffler,
                voter,
                Executors.newFixedThreadPool(4),
                4,
                5000L
        );
    }

    @Test
    void testExecuteEnsembleParallelExecution() {
        ClaimMetadata claim = ClaimMetadata.builder()
                .claimId("CLM-TEST-001")
                .payerId("BCBS")
                .cptCodes(List.of("93000"))
                .primaryIcd10("R07.9")
                .build();

        RcmExemplar ex = RcmExemplar.builder()
                .cptCode("93000")
                .groundTruthDecision(ReviewDecision.PAID)
                .build();

        EnsembleConsensusResult result = orchestrator.executeEnsemble(
                claim,
                null,
                List.of(ex),
                prompt -> LlmAuditResponseDto.builder()
                        .decision(ReviewDecision.PAID)
                        .denialRisk(DenialRisk.LOW_RISK)
                        .riskScore(0.08)
                        .stepByStepRationale("Prompt evaluated cleanly: " + prompt.substring(0, 20))
                        .build()
        );

        assertNotNull(result);
        assertEquals(ReviewDecision.PAID, result.getConsensusDecision());
        assertEquals(DenialRisk.LOW_RISK, result.getConsensusDenialRisk());
        assertEquals(4, result.getTotalEnsembleRuns());
        assertEquals(1.0, result.getAgreementConfidenceScore());
    }
}
