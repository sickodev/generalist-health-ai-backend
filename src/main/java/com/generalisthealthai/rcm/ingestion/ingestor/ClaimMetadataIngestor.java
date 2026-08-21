package com.generalisthealthai.rcm.ingestion.ingestor;

import com.generalisthealthai.rcm.domain.model.ClaimMetadata;
import com.generalisthealthai.rcm.domain.model.EligibilityRecord;
import com.generalisthealthai.rcm.ingestion.dto.ClaimIngestRequestDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ClaimMetadataIngestor {

    /**
     * Ingests and normalizes a ClaimIngestRequestDto into internal ClaimMetadata.
     */
    public ClaimMetadata ingest(ClaimIngestRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("Claim ingest request cannot be null");
        }

        String claimId = (request.getClaimId() != null && !request.getClaimId().trim().isEmpty())
                ? request.getClaimId().trim()
                : "CLM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        String payerId = request.getPayerId() != null ? request.getPayerId().trim() : "UNKNOWN_PAYER";
        String patientId = request.getPatientId() != null ? request.getPatientId().trim() : "UNKNOWN_PATIENT";
        String dateOfService = request.getDateOfService() != null ? request.getDateOfService().trim() : null;
        String placeOfService = request.getPlaceOfService() != null ? request.getPlaceOfService().trim() : "11";

        List<String> normalizedCptCodes = (request.getCptCodes() != null)
                ? request.getCptCodes().stream()
                .filter(c -> c != null && !c.trim().isEmpty())
                .map(String::trim)
                .collect(Collectors.toList())
                : new ArrayList<>();

        String primaryIcd10 = normalizeIcd10(request.getPrimaryIcd10());

        List<String> secondaryIcd10s = (request.getSecondaryIcd10s() != null)
                ? request.getSecondaryIcd10s().stream()
                .filter(c -> c != null && !c.trim().isEmpty())
                .map(this::normalizeIcd10)
                .collect(Collectors.toList())
                : new ArrayList<>();

        return ClaimMetadata.builder()
                .claimId(claimId)
                .payerId(payerId)
                .patientId(patientId)
                .dateOfService(dateOfService)
                .placeOfService(placeOfService)
                .cptCodes(normalizedCptCodes)
                .primaryIcd10(primaryIcd10)
                .secondaryIcd10s(secondaryIcd10s)
                .billedAmount(request.getBilledAmount())
                .build();
    }

    /**
     * Converts ClaimMetadata and optional EligibilityRecord into a canonical text string for embedding.
     */
    public String buildCanonicalEmbeddingText(ClaimMetadata claim, EligibilityRecord eligibility) {
        StringBuilder sb = new StringBuilder();
        sb.append("Payer: ").append(claim.getPayerId());
        sb.append(" | CPT: ").append(String.join(", ", claim.getCptCodes()));
        sb.append(" | Primary Diagnosis: ").append(claim.getPrimaryIcd10());

        if (claim.getSecondaryIcd10s() != null && !claim.getSecondaryIcd10s().isEmpty()) {
            sb.append(" | Secondary Diagnoses: ").append(String.join(", ", claim.getSecondaryIcd10s()));
        }

        if (claim.getPlaceOfService() != null) {
            sb.append(" | Place of Service: ").append(claim.getPlaceOfService());
        }

        if (eligibility != null) {
            sb.append(" | Coverage: ").append(eligibility.getCoverageStatus());
            sb.append(" | Network: ").append(eligibility.getNetworkStatus());
            sb.append(" | PA Required: ").append(eligibility.getPriorAuthRequired());
            if (eligibility.getCopayAmount() != null) {
                sb.append(" | Copay: $").append(eligibility.getCopayAmount());
            }
        }

        return sb.toString();
    }

    private String normalizeIcd10(String icd) {
        if (icd == null || icd.trim().isEmpty()) {
            return "UNKNOWN";
        }
        return icd.trim().toUpperCase();
    }
}
