package com.generalisthealthai.rcm.medprompt.retrieval;

import com.generalisthealthai.rcm.domain.enums.ReviewDecision;
import com.generalisthealthai.rcm.domain.model.RcmExemplar;
import com.generalisthealthai.rcm.infrastructure.persistence.RcmExemplarRepository;
import com.generalisthealthai.rcm.medprompt.embedding.EmbeddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class VectorSearchServiceTest {

    private RcmExemplarRepository exemplarRepository;
    private EmbeddingService embeddingService;
    private VectorSearchService vectorSearchService;

    @BeforeEach
    void setUp() {
        exemplarRepository = Mockito.mock(RcmExemplarRepository.class);
        embeddingService = Mockito.mock(EmbeddingService.class);
        vectorSearchService = new VectorSearchService(exemplarRepository, embeddingService, 4);
    }

    @Test
    void testCosineSimilarityCalculation() {
        float[] v1 = new float[]{1.0f, 0.0f, 0.0f};
        float[] v2 = new float[]{1.0f, 0.0f, 0.0f};
        assertEquals(1.0, VectorSearchService.calculateCosineSimilarity(v1, v2), 0.0001);

        float[] orthogonal = new float[]{0.0f, 1.0f, 0.0f};
        assertEquals(0.0, VectorSearchService.calculateCosineSimilarity(v1, orthogonal), 0.0001);
    }

    @Test
    void testFindTopKInMemoryRetrieval() {
        float[] queryVec = new float[]{1.0f, 0.0f, 0.0f};

        RcmExemplar ex1 = RcmExemplar.builder()
                .cptCode("93000")
                .groundTruthDecision(ReviewDecision.PAID)
                .build();
        ex1.setEmbeddingArray(new float[]{0.9f, 0.1f, 0.0f}); // High similarity

        RcmExemplar ex2 = RcmExemplar.builder()
                .cptCode("73721")
                .groundTruthDecision(ReviewDecision.DENIED)
                .build();
        ex2.setEmbeddingArray(new float[]{0.1f, 0.9f, 0.0f}); // Low similarity

        when(exemplarRepository.findAll()).thenReturn(List.of(ex2, ex1));
        when(embeddingService.vectorToString(queryVec)).thenReturn("[1.0,0.0,0.0]");
        when(embeddingService.embed(anyString())).thenReturn(queryVec);

        List<RcmExemplar> results = vectorSearchService.findTopKExemplars(queryVec, 2);

        assertEquals(2, results.size());
        assertEquals("93000", results.get(0).getCptCode()); // ex1 should be first
        assertEquals("73721", results.get(1).getCptCode());
    }
}
