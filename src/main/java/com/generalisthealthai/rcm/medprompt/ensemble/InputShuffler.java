package com.generalisthealthai.rcm.medprompt.ensemble;

import com.generalisthealthai.rcm.domain.model.ClaimMetadata;
import com.generalisthealthai.rcm.domain.model.EligibilityRecord;
import com.generalisthealthai.rcm.domain.model.RcmExemplar;
import com.generalisthealthai.rcm.medprompt.prompt.PromptBuilder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Component
public class InputShuffler {

    private final PromptBuilder promptBuilder;

    public InputShuffler(PromptBuilder promptBuilder) {
        this.promptBuilder = promptBuilder;
    }

    /**
     * Generates count distinct prompt variants by permuting exemplar order, diagnosis ordering, and procedure ordering.
     */
    public List<String> generatePromptVariants(
            ClaimMetadata claim,
            EligibilityRecord eligibility,
            List<RcmExemplar> exemplars,
            int count) {

        int variantCount = Math.max(1, count);
        List<String> variants = new ArrayList<>(variantCount);

        for (int i = 0; i < variantCount; i++) {
            // Permute exemplars
            List<RcmExemplar> shuffledExemplars = permuteExemplars(exemplars, i);

            // Permute claim metadata (secondary ICD-10s and CPT order)
            ClaimMetadata shuffledClaim = permuteClaim(claim, i);

            String prompt = promptBuilder.buildDynamicFewShotPrompt(shuffledClaim, eligibility, shuffledExemplars);
            variants.add(prompt);
        }

        return variants;
    }

    private List<RcmExemplar> permuteExemplars(List<RcmExemplar> original, int seed) {
        if (original == null || original.isEmpty()) {
            return new ArrayList<>();
        }
        List<RcmExemplar> copy = new ArrayList<>(original);
        if (seed == 0 || copy.size() <= 1) {
            return copy;
        }

        // Deterministic cyclic shift / shuffle based on seed
        int shift = seed % copy.size();
        Collections.rotate(copy, shift);
        return copy;
    }

    private ClaimMetadata permuteClaim(ClaimMetadata original, int seed) {
        if (original == null) {
            return null;
        }

        List<String> cptCodes = new ArrayList<>(original.getCptCodes());
        if (seed > 0 && cptCodes.size() > 1) {
            Collections.rotate(cptCodes, seed % cptCodes.size());
        }

        List<String> secondaryIcd10s = new ArrayList<>(original.getSecondaryIcd10s());
        if (seed > 0 && secondaryIcd10s.size() > 1) {
            Collections.rotate(secondaryIcd10s, seed % secondaryIcd10s.size());
        }

        return ClaimMetadata.builder()
                .claimId(original.getClaimId())
                .payerId(original.getPayerId())
                .patientId(original.getPatientId())
                .dateOfService(original.getDateOfService())
                .placeOfService(original.getPlaceOfService())
                .cptCodes(cptCodes)
                .primaryIcd10(original.getPrimaryIcd10())
                .secondaryIcd10s(secondaryIcd10s)
                .billedAmount(original.getBilledAmount())
                .build();
    }
}
