package com.generalisthealthai.rcm.validation;

import com.generalisthealthai.rcm.domain.enums.ReviewDecision;
import com.generalisthealthai.rcm.domain.model.ClaimMetadata;
import com.generalisthealthai.rcm.domain.model.RcmExemplar;
import com.generalisthealthai.rcm.ingestion.ingestor.ClaimMetadataIngestor;
import com.generalisthealthai.rcm.medprompt.ensemble.EnsembleOrchestrator;
import com.generalisthealthai.rcm.medprompt.ensemble.dto.EnsembleConsensusResult;
import com.generalisthealthai.rcm.medprompt.llm.LlmRouter;
import com.generalisthealthai.rcm.medprompt.retrieval.VectorSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MedpromptValidationService {

    private static final Logger log = LoggerFactory.getLogger(MedpromptValidationService.class);

    private final SyntheticClaimDatasetGenerator datasetGenerator;
    private final ClaimMetadataIngestor claimIngestor;
    private final VectorSearchService vectorSearchService;
    private final EnsembleOrchestrator ensembleOrchestrator;
    private final LlmRouter llmRouter;

    public MedpromptValidationService(
            SyntheticClaimDatasetGenerator datasetGenerator,
            ClaimMetadataIngestor claimIngestor,
            VectorSearchService vectorSearchService,
            EnsembleOrchestrator ensembleOrchestrator,
            LlmRouter llmRouter) {
        this.datasetGenerator = datasetGenerator;
        this.claimIngestor = claimIngestor;
        this.vectorSearchService = vectorSearchService;
        this.ensembleOrchestrator = ensembleOrchestrator;
        this.llmRouter = llmRouter;
    }

    /**
     * Executes the comprehensive 200-claim Medprompt validation suite and returns the statistical evaluation report.
     */
    public BenchmarkEvaluationReport runValidationSuite() {
        List<BenchmarkClaimRecord> dataset = datasetGenerator.generateBenchmarkDataset();
        log.info("Starting Medprompt validation suite over {} benchmark claims", dataset.size());

        long startTime = System.currentTimeMillis();

        int correct = 0;
        int tp = 0; // expected DENIED, predicted DENIED
        int fp = 0; // expected PAID, predicted DENIED
        int tn = 0; // expected PAID, predicted PAID
        int fn = 0; // expected DENIED, predicted PAID

        double sumConfidence = 0.0;
        List<BenchmarkEvaluationReport.BenchmarkResultItem> itemResults = new ArrayList<>(dataset.size());

        for (BenchmarkClaimRecord record : dataset) {
            ClaimMetadata claim = claimIngestor.ingest(record.getClaim());
            String canonical = claimIngestor.buildCanonicalEmbeddingText(claim, null);
            List<RcmExemplar> exemplars = vectorSearchService.findTopKExemplarsForText(canonical, 4);

            EnsembleConsensusResult consensus = ensembleOrchestrator.executeEnsemble(
                    claim,
                    null,
                    exemplars,
                    llmRouter.getInferenceFunction()
            );

            ReviewDecision predicted = consensus.getConsensusDecision();
            ReviewDecision expected = record.getExpectedDecision();
            boolean match = (predicted == expected);

            if (match) {
                correct++;
            }

            if (expected == ReviewDecision.DENIED && predicted == ReviewDecision.DENIED) {
                tp++;
            } else if (expected == ReviewDecision.PAID && predicted == ReviewDecision.DENIED) {
                fp++;
            } else if (expected == ReviewDecision.PAID && predicted == ReviewDecision.PAID) {
                tn++;
            } else if (expected == ReviewDecision.DENIED && predicted == ReviewDecision.PAID) {
                fn++;
            }

            sumConfidence += consensus.getAgreementConfidenceScore();

            itemResults.add(BenchmarkEvaluationReport.BenchmarkResultItem.builder()
                    .testId(record.getTestId())
                    .claimId(record.getClaim().getClaimId())
                    .expectedDecision(expected.name())
                    .predictedDecision(predicted.name())
                    .expectedDenialCode(record.getExpectedDenialCode())
                    .predictedDenialCodes(consensus.getConsensusDenialCodes())
                    .match(match)
                    .confidence(consensus.getAgreementConfidenceScore())
                    .build());
        }

        long durationMs = System.currentTimeMillis() - startTime;
        int total = dataset.size();

        double accuracy = ((double) correct / total) * 100.0;
        double precision = (tp + fp > 0) ? ((double) tp / (tp + fp)) : 1.0;
        double recall = (tp + fn > 0) ? ((double) tp / (tp + fn)) : 1.0;
        double f1 = (precision + recall > 0) ? (2.0 * (precision * recall) / (precision + recall)) : 0.0;
        double avgConf = (total > 0) ? (sumConfidence / total) : 0.0;

        BenchmarkEvaluationReport report = BenchmarkEvaluationReport.builder()
                .totalClaims(total)
                .correctPredictions(correct)
                .accuracyPercentage(Math.round(accuracy * 100.0) / 100.0)
                .truePositives(tp)
                .falsePositives(fp)
                .trueNegatives(tn)
                .falseNegatives(fn)
                .precision(Math.round(precision * 1000.0) / 1000.0)
                .recall(Math.round(recall * 1000.0) / 1000.0)
                .f1Score(Math.round(f1 * 1000.0) / 1000.0)
                .averageConfidence(Math.round(avgConf * 1000.0) / 1000.0)
                .executionDurationMs(durationMs)
                .individualResults(itemResults)
                .build();

        log.info("Medprompt validation complete: {}/{} correct (Accuracy: {}%, F1: {}, Duration: {}ms)",
                correct, total, report.getAccuracyPercentage(), report.getF1Score(), durationMs);

        return report;
    }
}
