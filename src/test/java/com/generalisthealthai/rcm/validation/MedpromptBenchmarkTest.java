package com.generalisthealthai.rcm.validation;

import com.generalisthealthai.rcm.ingestion.ingestor.ClaimMetadataIngestor;
import com.generalisthealthai.rcm.medprompt.embedding.EmbeddingService;
import com.generalisthealthai.rcm.medprompt.ensemble.ConsensusVoter;
import com.generalisthealthai.rcm.medprompt.ensemble.EnsembleOrchestrator;
import com.generalisthealthai.rcm.medprompt.ensemble.InputShuffler;
import com.generalisthealthai.rcm.medprompt.llm.GeminiClient;
import com.generalisthealthai.rcm.medprompt.llm.LlmRouter;
import com.generalisthealthai.rcm.medprompt.prompt.PromptBuilder;
import com.generalisthealthai.rcm.medprompt.retrieval.VectorSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class MedpromptBenchmarkTest {

    private MedpromptValidationService validationService;

    @BeforeEach
    void setUp() {
        SyntheticClaimDatasetGenerator datasetGenerator = new SyntheticClaimDatasetGenerator();
        ClaimMetadataIngestor claimIngestor = new ClaimMetadataIngestor();
        PromptBuilder promptBuilder = new PromptBuilder();
        InputShuffler inputShuffler = new InputShuffler(promptBuilder);
        ConsensusVoter consensusVoter = new ConsensusVoter();

        EnsembleOrchestrator orchestrator = new EnsembleOrchestrator(
                inputShuffler,
                consensusVoter,
                Executors.newFixedThreadPool(4),
                4,
                5000L
        );

        VectorSearchService vectorSearchService = Mockito.mock(VectorSearchService.class);
        when(vectorSearchService.findTopKExemplarsForText(anyString(), anyInt())).thenReturn(List.of());

        GeminiClient geminiClient = new GeminiClient(
                org.springframework.web.client.RestClient.builder(),
                new com.fasterxml.jackson.databind.ObjectMapper(),
                "demo-key",
                "https://generativelanguage.googleapis.com/v1beta",
                "gemini-1.5-pro",
                0.3
        );
        LlmRouter llmRouter = new LlmRouter(geminiClient);

        validationService = new MedpromptValidationService(
                datasetGenerator,
                claimIngestor,
                vectorSearchService,
                orchestrator,
                llmRouter
        );
    }

    @Test
    void test200ClaimValidationBenchmarkAccuracyMeetsTarget() {
        BenchmarkEvaluationReport report = validationService.runValidationSuite();

        assertNotNull(report);
        assertEquals(200, report.getTotalClaims());

        // Target requirement: Overall accuracy >= 85%
        assertTrue(report.getAccuracyPercentage() >= 85.0,
                "Accuracy should exceed 85%, was " + report.getAccuracyPercentage() + "%");

        // F1-Score >= 0.85
        assertTrue(report.getF1Score() >= 0.85,
                "F1 score should exceed 0.85, was " + report.getF1Score());

        // Precision and recall metrics
        assertTrue(report.getPrecision() >= 0.85, "Precision should exceed 0.85, was " + report.getPrecision());
        assertTrue(report.getRecall() >= 0.85, "Recall should exceed 0.85, was " + report.getRecall());
    }

    private void assertEquals(int expected, int actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }
}
