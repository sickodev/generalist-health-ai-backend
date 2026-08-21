package com.generalisthealthai.rcm.medprompt.ensemble;

import com.generalisthealthai.rcm.domain.enums.DenialRisk;
import com.generalisthealthai.rcm.domain.enums.ReviewDecision;
import com.generalisthealthai.rcm.medprompt.ensemble.dto.EnsembleConsensusResult;
import com.generalisthealthai.rcm.medprompt.ensemble.dto.LlmAuditResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class ConsensusVoter {

    private static final Logger log = LoggerFactory.getLogger(ConsensusVoter.class);

    /**
     * Aggregates multiple LLM inference runs into a single robust consensus audit report.
     */
    public EnsembleConsensusResult vote(List<LlmAuditResponseDto> runs) {
        if (runs == null || runs.isEmpty()) {
            log.warn("Consensus voter received 0 runs, returning safe default");
            return EnsembleConsensusResult.builder()
                    .consensusDecision(ReviewDecision.UNKNOWN)
                    .consensusDenialRisk(DenialRisk.HIGH_RISK)
                    .averageRiskScore(1.0)
                    .agreementConfidenceScore(0.0)
                    .totalEnsembleRuns(0)
                    .agreeingRuns(0)
                    .primaryRationale("No model runs succeeded to produce consensus.")
                    .build();
        }

        int totalRuns = runs.size();

        // 1. Majority Voting on ReviewDecision
        Map<ReviewDecision, Integer> voteCounts = new EnumMap<>(ReviewDecision.class);
        for (LlmAuditResponseDto run : runs) {
            ReviewDecision dec = run.getDecision() != null ? run.getDecision() : ReviewDecision.UNKNOWN;
            voteCounts.put(dec, voteCounts.getOrDefault(dec, 0) + 1);
        }

        ReviewDecision winnerDecision = voteCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(ReviewDecision.UNKNOWN);

        int agreeingCount = voteCounts.getOrDefault(winnerDecision, 1);
        double agreementConfidence = (double) agreeingCount / totalRuns;

        // 2. Average Risk Score
        double sumRiskScore = 0.0;
        int riskCount = 0;
        for (LlmAuditResponseDto run : runs) {
            if (run.getRiskScore() != null) {
                sumRiskScore += run.getRiskScore();
                riskCount++;
            }
        }
        double avgRiskScore = (riskCount > 0) ? (sumRiskScore / riskCount) : (winnerDecision == ReviewDecision.DENIED ? 0.85 : 0.15);
        avgRiskScore = Math.round(avgRiskScore * 100.0) / 100.0;

        // 3. Consensus Denial Risk Level
        DenialRisk consensusRisk;
        if (avgRiskScore >= 0.70 || winnerDecision == ReviewDecision.DENIED) {
            consensusRisk = DenialRisk.HIGH_RISK;
        } else if (avgRiskScore >= 0.35 || winnerDecision == ReviewDecision.PA_REQUIRED) {
            consensusRisk = DenialRisk.MODERATE_RISK;
        } else {
            consensusRisk = DenialRisk.LOW_RISK;
        }

        // 4. Aggregate Predicted Denial Codes
        Set<String> denialCodes = new LinkedHashSet<>();
        for (LlmAuditResponseDto run : runs) {
            if (run.getPredictedDenialCodes() != null) {
                for (String code : run.getPredictedDenialCodes()) {
                    if (code != null && !code.isBlank()) {
                        denialCodes.add(code.trim().toUpperCase());
                    }
                }
            }
        }

        // 5. Select Best Primary Rationale & Appeal Letter Draft from matching runs
        String bestRationale = null;
        String bestAppeal = null;

        List<LlmAuditResponseDto> matchingRuns = new ArrayList<>();
        for (LlmAuditResponseDto run : runs) {
            if (run.getDecision() == winnerDecision) {
                matchingRuns.add(run);
            }
        }

        List<LlmAuditResponseDto> pool = !matchingRuns.isEmpty() ? matchingRuns : runs;
        for (LlmAuditResponseDto candidate : pool) {
            if (candidate.getStepByStepRationale() != null && !candidate.getStepByStepRationale().isBlank()) {
                if (bestRationale == null || candidate.getStepByStepRationale().length() > bestRationale.length()) {
                    bestRationale = candidate.getStepByStepRationale();
                }
            }
            if (candidate.getAppealLetterDraft() != null && !candidate.getAppealLetterDraft().isBlank()) {
                if (bestAppeal == null || candidate.getAppealLetterDraft().length() > bestAppeal.length()) {
                    bestAppeal = candidate.getAppealLetterDraft();
                }
            }
        }

        if (bestRationale == null) {
            bestRationale = "Audit complete. Outcome evaluated as " + winnerDecision;
        }

        // 6. Average Patient Responsibility
        BigDecimal totalResp = BigDecimal.ZERO;
        int respCount = 0;
        for (LlmAuditResponseDto run : runs) {
            if (run.getPatientResponsibility() != null) {
                totalResp = totalResp.add(run.getPatientResponsibility());
                respCount++;
            }
        }
        BigDecimal avgResp = (respCount > 0)
                ? totalResp.divide(BigDecimal.valueOf(respCount), 2, RoundingMode.HALF_UP)
                : null;

        return EnsembleConsensusResult.builder()
                .consensusDecision(winnerDecision)
                .consensusDenialRisk(consensusRisk)
                .averageRiskScore(avgRiskScore)
                .consensusDenialCodes(new ArrayList<>(denialCodes))
                .primaryRationale(bestRationale)
                .appealLetterDraft(bestAppeal)
                .averagePatientResponsibility(avgResp)
                .agreementConfidenceScore(Math.round(agreementConfidence * 100.0) / 100.0)
                .totalEnsembleRuns(totalRuns)
                .agreeingRuns(agreeingCount)
                .individualRuns(runs)
                .build();
    }
}
