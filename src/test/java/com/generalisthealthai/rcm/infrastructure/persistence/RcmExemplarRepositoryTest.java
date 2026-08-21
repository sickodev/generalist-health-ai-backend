package com.generalisthealthai.rcm.infrastructure.persistence;

import com.generalisthealthai.rcm.domain.enums.ReviewDecision;
import com.generalisthealthai.rcm.domain.model.RcmExemplar;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("dev")
class RcmExemplarRepositoryTest {

    @Autowired
    private RcmExemplarRepository exemplarRepository;

    @Test
    void testSaveAndRetrieveExemplarWithEmbedding() {
        RcmExemplar exemplar = RcmExemplar.builder()
                .payerId("UHC")
                .cptCode("99214")
                .icd10Code("M54.5")
                .serviceDescription("Office outpatient visit 30-39 min")
                .groundTruthDecision(ReviewDecision.PAID)
                .validatedCoTRationale("1. Diagnosis aligns with procedure. 2. Medical necessity established. 3. Outcome: PAID.")
                .build();

        float[] testEmbedding = new float[]{0.123f, -0.456f, 0.789f};
        exemplar.setEmbeddingArray(testEmbedding);

        RcmExemplar saved = exemplarRepository.save(exemplar);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());

        Optional<RcmExemplar> found = exemplarRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("UHC", found.get().getPayerId());
        assertEquals(ReviewDecision.PAID, found.get().getGroundTruthDecision());

        float[] retrievedEmbedding = found.get().getEmbeddingArray();
        assertArrayEquals(testEmbedding, retrievedEmbedding, 0.0001f);

        List<RcmExemplar> byDecision = exemplarRepository.findByGroundTruthDecision(ReviewDecision.PAID);
        assertEquals(1, byDecision.size());
    }
}
