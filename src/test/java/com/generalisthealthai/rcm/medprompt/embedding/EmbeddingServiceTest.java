package com.generalisthealthai.rcm.medprompt.embedding;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmbeddingServiceTest {

    private EmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        embeddingService = new EmbeddingService(
                RestClient.builder(),
                new ObjectMapper(),
                "demo-key",
                "https://generativelanguage.googleapis.com/v1beta",
                "text-embedding-004",
                768
        );
    }

    @Test
    void testEmbedReturnsNormalized768DimensionVector() {
        String text = "Payer: BCBS | CPT: 93000 | Diagnosis: R07.9 (Chest pain)";
        float[] vector = embeddingService.embed(text);

        assertNotNull(vector);
        assertEquals(768, vector.length);

        // Verify normalized vector length (~ 1.0)
        double normSq = 0.0;
        for (float v : vector) {
            normSq += v * v;
        }
        assertTrue(Math.abs(normSq - 1.0) < 0.01, "Vector should be normalized close to 1.0, was " + normSq);
    }

    @Test
    void testVectorToString() {
        float[] vector = new float[]{0.1f, 0.2f, 0.3f};
        String str = embeddingService.vectorToString(vector);
        assertEquals("[0.1,0.2,0.3]", str);
    }

    @Test
    void testEmbedNullOrEmptyReturnsZeroVector() {
        float[] vNull = embeddingService.embed(null);
        assertEquals(768, vNull.length);
        assertEquals(0.0f, vNull[0]);

        float[] vEmpty = embeddingService.embed("   ");
        assertEquals(768, vEmpty.length);
        assertEquals(0.0f, vEmpty[0]);
    }
}
