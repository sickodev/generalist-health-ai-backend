package com.generalisthealthai.rcm.medprompt.prompt;

import com.generalisthealthai.rcm.domain.model.ClaimMetadata;
import com.generalisthealthai.rcm.domain.model.EligibilityRecord;
import com.generalisthealthai.rcm.domain.model.RcmExemplar;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PromptBuilder {

    /**
     * Builds dynamic few-shot prompt with system instructions, retrieved exemplars with CoT, and target claim.
     */
    public String buildDynamicFewShotPrompt(ClaimMetadata claim, EligibilityRecord eligibility, List<RcmExemplar> exemplars) {
        StringBuilder sb = new StringBuilder();

        sb.append(PromptTemplate.SYSTEM_INSTRUCTION.trim()).append("\n\n");
        sb.append("====================================================\n");
        sb.append("HISTORICAL AUDIT EXEMPLARS (VERIFIED REASONING CHAINS)\n");
        sb.append("====================================================\n\n");

        if (exemplars != null && !exemplars.isEmpty()) {
            for (int i = 0; i < exemplars.size(); i++) {
                RcmExemplar ex = exemplars.get(i);
                sb.append(String.format("[START HISTORICAL EXEMPLAR %d]\n", i + 1));
                sb.append("Payer: ").append(ex.getPayerId() != null ? ex.getPayerId() : "General").append("\n");
                sb.append("Procedure (CPT): ").append(ex.getCptCode() != null ? ex.getCptCode() : "N/A");
                if (ex.getServiceDescription() != null && !ex.getServiceDescription().isBlank()) {
                    sb.append(" - ").append(ex.getServiceDescription());
                }
                sb.append("\n");
                sb.append("Diagnosis (ICD-10): ").append(ex.getIcd10Code() != null ? ex.getIcd10Code() : "N/A").append("\n");
                sb.append("Ground-Truth Outcome: ").append(ex.getGroundTruthDecision());
                if (ex.getDenialCode() != null && !ex.getDenialCode().isBlank()) {
                    sb.append(" (Code: ").append(ex.getDenialCode()).append(")");
                }
                sb.append("\n\n");
                sb.append("Validated Audit Rationale:\n");
                sb.append(ex.getValidatedCoTRationale() != null ? ex.getValidatedCoTRationale() : "N/A").append("\n");
                sb.append(String.format("[END HISTORICAL EXEMPLAR %d]\n\n", i + 1));
            }
        } else {
            sb.append("(No historical exemplars available for this combination)\n\n");
        }

        sb.append("====================================================\n");
        sb.append("TARGET CLAIM TO AUDIT\n");
        sb.append("====================================================\n");
        sb.append("Payer: ").append(claim.getPayerId()).append("\n");
        sb.append("Procedure Codes (CPT): ").append(String.join(", ", claim.getCptCodes())).append("\n");
        sb.append("Primary Diagnosis (ICD-10): ").append(claim.getPrimaryIcd10()).append("\n");

        if (claim.getSecondaryIcd10s() != null && !claim.getSecondaryIcd10s().isEmpty()) {
            sb.append("Secondary Diagnoses (ICD-10): ").append(String.join(", ", claim.getSecondaryIcd10s())).append("\n");
        }

        if (claim.getPlaceOfService() != null) {
            sb.append("Place of Service Code: ").append(claim.getPlaceOfService()).append("\n");
        }

        if (claim.getDateOfService() != null) {
            sb.append("Date of Service: ").append(claim.getDateOfService()).append("\n");
        }

        if (claim.getBilledAmount() != null) {
            sb.append("Billed Amount: $").append(claim.getBilledAmount()).append("\n");
        }

        if (eligibility != null) {
            sb.append("\n--- Patient Eligibility Status ---\n");
            sb.append("Coverage Status: ").append(eligibility.getCoverageStatus()).append("\n");
            sb.append("Network Status: ").append(eligibility.getNetworkStatus()).append("\n");
            sb.append("Prior Authorization Required by Plan: ").append(eligibility.getPriorAuthRequired()).append("\n");
            if (eligibility.getCopayAmount() != null) {
                sb.append("Plan Copay: $").append(eligibility.getCopayAmount()).append("\n");
            }
            if (eligibility.getCoinsurancePercent() != null) {
                sb.append("Plan Coinsurance: ").append(eligibility.getCoinsurancePercent()).append("\n");
            }
        }

        sb.append("\n====================================================\n");
        sb.append("TASK: Execute step-by-step Chain-of-Thought (CoT) audit rationale, determine if the target claim will be PAID, DENIED, or requires PA, and output the required JSON object:\n");

        return sb.toString();
    }

    /**
     * Builds prompt for automated offline generation of Chain-of-Thought reasoning from ground truth claims.
     */
    public String buildCoTGenerationPrompt(
            String payerId,
            String cptCode,
            String icd10Code,
            String serviceDescription,
            String groundTruthOutcome,
            String groundTruthDenialCode) {

        StringBuilder sb = new StringBuilder();
        sb.append("You are an expert Medical Auditor creating audit training exemplars for an RCM intelligence engine.\n");
        sb.append("Review this historic healthcare claim with a known verified payment outcome:\n\n");
        sb.append("Payer: ").append(payerId).append("\n");
        sb.append("CPT Procedure: ").append(cptCode);
        if (serviceDescription != null && !serviceDescription.isBlank()) {
            sb.append(" (").append(serviceDescription).append(")");
        }
        sb.append("\n");
        sb.append("ICD-10 Diagnosis: ").append(icd10Code).append("\n");
        sb.append("Ground Truth Outcome: ").append(groundTruthOutcome);
        if (groundTruthDenialCode != null && !groundTruthDenialCode.isBlank()) {
            sb.append(" (Denial Code: ").append(groundTruthDenialCode).append(")");
        }
        sb.append("\n\n");
        sb.append("Write a concise, rigorous 3 to 4 step Chain-of-Thought (CoT) audit rationale explaining why this claim resulted in this outcome based on payer medical necessity, coding rules, or pre-authorization policies.\n");
        sb.append("Format as numbered steps ending with the final decision.");

        return sb.toString();
    }
}
