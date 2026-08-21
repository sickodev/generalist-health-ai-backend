package com.generalisthealthai.rcm.medprompt.ensemble.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class LlmAuditResponseDto {

    private ReviewDecision decision;
    private DenialRisk denialRisk;
    private Double riskScore;

    @Builder.Default
    private List<String> predictedDenialCodes = new ArrayList<>();

    private String stepByStepRationale;
    private String appealLetterDraft;
    private BigDecimal patientResponsibility;
    private Double confidenceScore;
    private Integer variantIndex;
}
