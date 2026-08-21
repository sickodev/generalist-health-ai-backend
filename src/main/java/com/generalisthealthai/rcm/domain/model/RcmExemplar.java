package com.generalisthealthai.rcm.domain.model;

import com.generalisthealthai.rcm.domain.enums.ReviewDecision;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "rcm_exemplars")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RcmExemplar {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "payer_id", length = 64)
    private String payerId;

    @Column(name = "cpt_code", length = 32)
    private String cptCode;

    @Column(name = "icd10_code", length = 32)
    private String icd10Code;

    @Column(name = "service_description", length = 512)
    private String serviceDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "ground_truth_decision", nullable = false, length = 32)
    private ReviewDecision groundTruthDecision;

    @Column(name = "denial_code", length = 32)
    private String denialCode;

    @Column(name = "validated_cot_rationale", columnDefinition = "TEXT")
    private String validatedCoTRationale;

    /**
     * Stored in string format '[0.123, 0.456, ...]' compatible with pgvector and fallback text.
     */
    @Column(name = "embedding", columnDefinition = "TEXT")
    private String embedding;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    @Transient
    public float[] getEmbeddingArray() {
        if (embedding == null || embedding.trim().isEmpty()) {
            return new float[0];
        }
        String clean = embedding.replaceAll("[\\[\\]\\s]", "");
        if (clean.isEmpty()) {
            return new float[0];
        }
        String[] parts = clean.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                result[i] = Float.parseFloat(parts[i]);
            } catch (NumberFormatException e) {
                result[i] = 0.0f;
            }
        }
        return result;
    }

    @Transient
    public void setEmbeddingArray(float[] vector) {
        if (vector == null || vector.length == 0) {
            this.embedding = "[]";
            return;
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        this.embedding = sb.toString();
    }
}
