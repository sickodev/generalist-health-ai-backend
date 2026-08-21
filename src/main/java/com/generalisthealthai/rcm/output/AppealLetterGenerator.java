package com.generalisthealthai.rcm.output;

import com.generalisthealthai.rcm.domain.model.ClaimMetadata;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class AppealLetterGenerator {

    /**
     * Generates a formal, payer-tailored appeal letter for a denied claim or high denial-risk claim.
     */
    public String generateAppealLetter(
            ClaimMetadata claim,
            List<String> denialCodes,
            String auditRationale,
            String customPayerPolicy) {

        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String payer = claim.getPayerId() != null ? claim.getPayerId() : "Health Insurance Plan";
        String claimId = claim.getClaimId() != null ? claim.getClaimId() : "N/A";
        String patientId = claim.getPatientId() != null ? claim.getPatientId() : "N/A";
        String dos = claim.getDateOfService() != null ? claim.getDateOfService() : today;
        String cpts = (claim.getCptCodes() != null) ? String.join(", ", claim.getCptCodes()) : "N/A";
        String icd10 = claim.getPrimaryIcd10() != null ? claim.getPrimaryIcd10() : "N/A";
        String denialCodeStr = (denialCodes != null && !denialCodes.isEmpty())
                ? String.join(", ", denialCodes)
                : "Medical Necessity / Documentation Requirement";

        StringBuilder sb = new StringBuilder();
        sb.append("DATE: ").append(today).append("\n\n");
        sb.append("TO: ").append(payer).append(" - Appeals & Grievance Department\n");
        sb.append("RE: FORMAL CLAIM APPEAL & REQUEST FOR RECONSIDERATION\n\n");
        sb.append("PATIENT / MEMBER ID: ").append(patientId).append("\n");
        sb.append("CLAIM REFERENCE NUMBER: ").append(claimId).append("\n");
        sb.append("DATE OF SERVICE: ").append(dos).append("\n");
        sb.append("PROCEDURE CODE(S) (CPT): ").append(cpts).append("\n");
        sb.append("PRIMARY DIAGNOSIS (ICD-10): ").append(icd10).append("\n");
        sb.append("DISPUTED DENIAL CODE(S): ").append(denialCodeStr).append("\n\n");

        sb.append("Dear Claims Appeals Committee,\n\n");
        sb.append("We are writing to formally appeal the denial of payment for the healthcare services rendered on ")
                .append(dos).append(" to the above-referenced member. The claim was denied citing ").append(denialCodeStr).append(".\n\n");

        sb.append("CLINICAL RATIONALE & JUSTIFICATION:\n");
        if (auditRationale != null && !auditRationale.isBlank()) {
            sb.append(auditRationale.trim()).append("\n\n");
        } else {
            sb.append("The rendered procedure was medically necessary and directly indicated by the patient's presenting clinical condition. ")
                    .append("All standard coding and regulatory guidelines were complied with in full.\n\n");
        }

        if (customPayerPolicy != null && !customPayerPolicy.isBlank()) {
            sb.append("RELEVANT COVERAGE POLICY GUIDELINES:\n")
                    .append(customPayerPolicy.trim()).append("\n\n");
        }

        sb.append("SUPPORTING DOCUMENTATION ENCLOSED:\n");
        sb.append("1. Complete clinical progress notes and provider evaluation for Date of Service ").append(dos).append("\n");
        sb.append("2. Order and rationale from the attending physician\n");
        sb.append("3. Itemized billing statement and CMS-1500 / UB-04 claim form\n\n");

        sb.append("Based on the clinical evidence and policy criteria, we respectfully request that you reverse the denial and reprocess this claim for full payment.\n\n");
        sb.append("Sincerely,\n");
        sb.append("Revenue Cycle & Medical Billing Department\n");
        sb.append("Healthcare Provider Billing Office\n");

        return sb.toString();
    }
}
