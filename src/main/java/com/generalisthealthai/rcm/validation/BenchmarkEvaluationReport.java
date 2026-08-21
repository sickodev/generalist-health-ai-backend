package com.generalisthealthai.rcm.validation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BenchmarkEvaluationReport {

    private int totalClaims;
    private int correctPredictions;
    private double accuracyPercentage;
    private int truePositives;   // Correctly predicted DENIED
    private int falsePositives;  // Predicted DENIED when actually PAID
    private int trueNegatives;   // Correctly predicted PAID
    private int falseNegatives;  // Predicted PAID when actually DENIED
    private double precision;
    private double recall;
    private double f1Score;
    private double averageConfidence;
    private long executionDurationMs;

    @Builder.Default
    private List<BenchmarkResultItem> individualResults = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BenchmarkResultItem {
        private String testId;
        private String claimId;
        private String expectedDecision;
        private String predictedDecision;
        private String expectedDenialCode;
        private List<String> predictedDenialCodes;
        private boolean match;
        private double confidence;
    }
}
