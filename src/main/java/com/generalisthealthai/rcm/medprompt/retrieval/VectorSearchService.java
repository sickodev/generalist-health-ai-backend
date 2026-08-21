package com.generalisthealthai.rcm.medprompt.retrieval;

import com.generalisthealthai.rcm.domain.model.RcmExemplar;
import com.generalisthealthai.rcm.infrastructure.persistence.RcmExemplarRepository;
import com.generalisthealthai.rcm.medprompt.embedding.EmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VectorSearchService {

    private static final Logger log = LoggerFactory.getLogger(VectorSearchService.class);

    private final RcmExemplarRepository exemplarRepository;
    private final EmbeddingService embeddingService;
    private final int defaultK;

    public VectorSearchService(
            RcmExemplarRepository exemplarRepository,
            EmbeddingService embeddingService,
            @Value("${rcm.medprompt.knn-k:4}") int defaultK) {
        this.exemplarRepository = exemplarRepository;
        this.embeddingService = embeddingService;
        this.defaultK = defaultK;
    }

    /**
     * Finds top k nearest historical RCM exemplars for the given query text.
     */
    public List<RcmExemplar> findTopKExemplarsForText(String text, int k) {
        int limit = (k > 0) ? k : defaultK;
        float[] queryVector = embeddingService.embed(text);
        return findTopKExemplars(queryVector, limit);
    }

    /**
     * Finds top k nearest historical RCM exemplars for the given query embedding vector.
     */
    public List<RcmExemplar> findTopKExemplars(float[] queryVector, int k) {
        int limit = (k > 0) ? k : defaultK;
        String vectorString = embeddingService.vectorToString(queryVector);

        try {
            // Attempt native pgvector cosine search
            List<RcmExemplar> nativeResults = exemplarRepository.findNearestNeighborsNative(vectorString, limit);
            if (nativeResults != null && !nativeResults.isEmpty()) {
                log.debug("Retrieved {} exemplars via native pgvector search", nativeResults.size());
                return nativeResults;
            }
        } catch (Exception e) {
            log.debug("Native pgvector query failed ({}), falling back to in-memory cosine ranking", e.getMessage());
        }

        // In-memory fallback (used for H2 dev/test profile)
        return findTopKInMemory(queryVector, limit);
    }

    /**
     * In-memory cosine similarity search across all stored exemplars.
     */
    public List<RcmExemplar> findTopKInMemory(float[] queryVector, int limit) {
        List<RcmExemplar> allExemplars = exemplarRepository.findAll();
        if (allExemplars.isEmpty()) {
            log.warn("No RCM exemplars available in database for vector search");
            return List.of();
        }

        return allExemplars.stream()
                .sorted(Comparator.comparingDouble((RcmExemplar ex) ->
                        -calculateCosineSimilarity(queryVector, ex.getEmbeddingArray())))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Calculates cosine similarity between two float vectors.
     */
    public static double calculateCosineSimilarity(float[] v1, float[] v2) {
        if (v1 == null || v2 == null || v1.length == 0 || v2.length == 0) {
            return 0.0;
        }

        int minLen = Math.min(v1.length, v2.length);
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < minLen; i++) {
            dotProduct += v1[i] * v2[i];
            normA += v1[i] * v1[i];
            normB += v2[i] * v2[i];
        }

        if (normA <= 1e-8 || normB <= 1e-8) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
