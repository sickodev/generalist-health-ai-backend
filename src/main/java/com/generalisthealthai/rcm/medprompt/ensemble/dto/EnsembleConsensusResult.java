package com.generalisthealthai.rcm.medprompt.ensemble.dto;

import com.generalisthealthai.rcm.domain.enums.DenialRisk;
import com.generalisthealthai.rcm.domain.enums.ReviewDecision;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnsembleConsensusResult {

    private ReviewDecision consensusDecision;
    private DenialRisk consensusDenialRisk;
    private Double averageRiskScore;

    @Builder.Default
    private List<String> consensusDenialCodes = new ArrayList<>();

    private String primaryRationale;
    private String appealLetterDraft;
    private BigDecimal averagePatientResponsibility;
    private Double agreementConfidenceScore;
    private int totalEnsembleRuns;
    private int agreeingRuns;

    @Builder.Default
    private List<LlmAuditResponseDto> individualRuns = new ArrayList<>();
}
