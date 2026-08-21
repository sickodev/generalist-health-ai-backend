package com.generalisthealthai.rcm.domain.model;

import com.generalisthealthai.rcm.domain.converter.StringListJsonConverter;
import com.generalisthealthai.rcm.domain.enums.AuditType;
import com.generalisthealthai.rcm.domain.enums.JobStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "audit_jobs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    @Builder.Default
    private JobStatus status = JobStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "audit_type", nullable = false, length = 64)
    private AuditType auditType;

    @Column(name = "payer_id", length = 64)
    private String payerId;

    @Column(name = "patient_id", length = 64)
    private String patientId;

    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "cpt_codes", columnDefinition = "TEXT")
    @Builder.Default
    private List<String> cptCodes = new ArrayList<>();

    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "icd10_codes", columnDefinition = "TEXT")
    @Builder.Default
    private List<String> icd10Codes = new ArrayList<>();

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.status == null) {
            this.status = JobStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
        if (this.status == JobStatus.DONE || this.status == JobStatus.FAILED) {
            if (this.completedAt == null) {
                this.completedAt = Instant.now();
            }
        }
    }
}
