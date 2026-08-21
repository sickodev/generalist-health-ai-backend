package com.generalisthealthai.rcm.medprompt.ensemble;

import com.generalisthealthai.rcm.domain.enums.ReviewDecision;
import com.generalisthealthai.rcm.domain.model.ClaimMetadata;
import com.generalisthealthai.rcm.domain.model.EligibilityRecord;
import com.generalisthealthai.rcm.domain.model.RcmExemplar;
import com.generalisthealthai.rcm.medprompt.prompt.PromptBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class InputShufflerTest {

    private InputShuffler shuffler;

    @BeforeEach
    void setUp() {
        shuffler = new InputShuffler(new PromptBuilder());
    }

    @Test
    void testGenerateMultiplePromptVariants() {
        ClaimMetadata claim = ClaimMetadata.builder()
                .payerId("BCBS")
                .cptCodes(List.of("99214", "93000"))
                .primaryIcd10("M54.5")
                .secondaryIcd10s(List.of("R07.9", "I10"))
                .build();

        RcmExemplar ex1 = RcmExemplar.builder()
                .cptCode("99214")
                .groundTruthDecision(ReviewDecision.PAID)
                .build();
        RcmExemplar ex2 = RcmExemplar.builder()
                .cptCode("93000")
                .groundTruthDecision(ReviewDecision.DENIED)
                .build();

        List<String> variants = shuffler.generatePromptVariants(claim, null, List.of(ex1, ex2), 4);

        assertEquals(4, variants.size());
        for (String variant : variants) {
            assertNotNull(variant);
            assertNotNull(variant.contains("BCBS"));
        }
    }
}
