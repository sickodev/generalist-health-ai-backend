package com.generalisthealthai.rcm.domain.model;

import com.generalisthealthai.rcm.domain.converter.StringListJsonConverter;
import com.generalisthealthai.rcm.domain.enums.DenialRisk;
import com.generalisthealthai.rcm.domain.enums.ReviewDecision;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "audit_reports")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "job_id", nullable = false, unique = true)
    private UUID jobId;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 32)
    private ReviewDecision decision;

    @Enumerated(EnumType.STRING)
    @Column(name = "denial_risk", nullable = false, length = 32)
    private DenialRisk denialRisk;

    @Column(name = "risk_score", nullable = false)
    private Double riskScore;

    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "predicted_denial_codes", columnDefinition = "TEXT")
    @Builder.Default
    private List<String> predictedDenialCodes = new ArrayList<>();

    @Column(name = "audit_rationale", columnDefinition = "TEXT")
    private String auditRationale;

    @Column(name = "appeal_letter_draft", columnDefinition = "TEXT")
    private String appealLetterDraft;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "patient_responsibility_amount", precision = 12, scale = 2)
    private BigDecimal patientResponsibilityAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
