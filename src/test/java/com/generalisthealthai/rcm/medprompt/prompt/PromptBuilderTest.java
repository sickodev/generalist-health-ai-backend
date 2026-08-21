package com.generalisthealthai.rcm.medprompt.prompt;

import com.generalisthealthai.rcm.domain.enums.CoverageStatus;
import com.generalisthealthai.rcm.domain.enums.NetworkStatus;
import com.generalisthealthai.rcm.domain.enums.ReviewDecision;
import com.generalisthealthai.rcm.domain.model.ClaimMetadata;
import com.generalisthealthai.rcm.domain.model.EligibilityRecord;
import com.generalisthealthai.rcm.domain.model.RcmExemplar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptBuilderTest {

    private PromptBuilder promptBuilder;

    @BeforeEach
    void setUp() {
        promptBuilder = new PromptBuilder();
    }

    @Test
    void testBuildDynamicFewShotPrompt() {
        ClaimMetadata claim = ClaimMetadata.builder()
                .payerId("BCBS")
                .cptCodes(List.of("93000"))
                .primaryIcd10("R07.9")
                .secondaryIcd10s(List.of("I10"))
                .placeOfService("11")
                .billedAmount(new BigDecimal("350.00"))
                .build();

        EligibilityRecord eligibility = EligibilityRecord.builder()
                .coverageStatus(CoverageStatus.ACTIVE)
                .networkStatus(NetworkStatus.IN_NETWORK)
                .priorAuthRequired(false)
                .copayAmount(new BigDecimal("25.00"))
                .build();

        RcmExemplar exemplar = RcmExemplar.builder()
                .payerId("BCBS")
                .cptCode("93000")
                .icd10Code("R07.9")
                .serviceDescription("Electrocardiogram")
                .groundTruthDecision(ReviewDecision.PAID)
                .validatedCoTRationale("1. Chest pain justifies EKG. 2. Outcome: PAID.")
                .build();

        String prompt = promptBuilder.buildDynamicFewShotPrompt(claim, eligibility, List.of(exemplar));

        assertNotNull(prompt);
        assertTrue(prompt.contains("HISTORICAL AUDIT EXEMPLARS"));
        assertTrue(prompt.contains("[START HISTORICAL EXEMPLAR 1]"));
        assertTrue(prompt.contains("1. Chest pain justifies EKG. 2. Outcome: PAID."));
        assertTrue(prompt.contains("TARGET CLAIM TO AUDIT"));
        assertTrue(prompt.contains("Payer: BCBS"));
        assertTrue(prompt.contains("Procedure Codes (CPT): 93000"));
        assertTrue(prompt.contains("Primary Diagnosis (ICD-10): R07.9"));
        assertTrue(prompt.contains("Coverage Status: ACTIVE"));
        assertTrue(prompt.contains("Plan Copay: $25.00"));
    }

    @Test
    void testBuildCoTGenerationPrompt() {
        String prompt = promptBuilder.buildCoTGenerationPrompt(
                "UHC", "73721", "M25.561", "MRI Knee", "DENIED", "CO-197");

        assertNotNull(prompt);
        assertTrue(prompt.contains("Payer: UHC"));
        assertTrue(prompt.contains("CPT Procedure: 73721"));
        assertTrue(prompt.contains("ICD-10 Diagnosis: M25.561"));
        assertTrue(prompt.contains("Ground Truth Outcome: DENIED (Denial Code: CO-197)"));
        assertTrue(prompt.contains("Chain-of-Thought"));
    }
}
