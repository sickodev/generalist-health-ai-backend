package com.generalisthealthai.rcm.api.dto;

import com.generalisthealthai.rcm.domain.enums.DenialRisk;
import com.generalisthealthai.rcm.domain.enums.ReviewDecision;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditReportResponseDto {

    private UUID reportId;
    private UUID jobId;
    private ReviewDecision decision;
    private DenialRisk denialRisk;
    private Double riskScore;

    @Builder.Default
    private List<String> predictedDenialCodes = new ArrayList<>();

    private String auditRationale;
    private String appealLetterDraft;
    private Double confidenceScore;
    private BigDecimal patientResponsibilityAmount;
    private Instant createdAt;
}
