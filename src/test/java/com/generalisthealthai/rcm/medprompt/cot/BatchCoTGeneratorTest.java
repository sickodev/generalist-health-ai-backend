package com.generalisthealthai.rcm.medprompt.cot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.generalisthealthai.rcm.domain.enums.ReviewDecision;
import com.generalisthealthai.rcm.domain.model.RcmExemplar;
import com.generalisthealthai.rcm.infrastructure.persistence.RcmExemplarRepository;
import com.generalisthealthai.rcm.medprompt.embedding.EmbeddingService;
import com.generalisthealthai.rcm.medprompt.prompt.PromptBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BatchCoTGeneratorTest {

    private PromptBuilder promptBuilder;
    private RcmExemplarRepository exemplarRepository;
    private EmbeddingService embeddingService;
    private BatchCoTGenerator generator;

    @BeforeEach
    void setUp() {
        promptBuilder = new PromptBuilder();
        exemplarRepository = Mockito.mock(RcmExemplarRepository.class);
        embeddingService = Mockito.mock(EmbeddingService.class);

        when(embeddingService.embed(anyString())).thenReturn(new float[768]);
        when(exemplarRepository.save(any(RcmExemplar.class))).thenAnswer(invocation -> invocation.getArgument(0));

        generator = new BatchCoTGenerator(
                promptBuilder,
                exemplarRepository,
                embeddingService,
                RestClient.builder(),
                new ObjectMapper(),
                "demo-key",
                "https://generativelanguage.googleapis.com/v1beta",
                "gemini-1.5-pro"
        );
    }

    @Test
    void testGenerateAndValidateCoTPaidOutcome() {
        CoTValidationResult result = generator.generateAndValidateCoT(
                "BCBS", "93000", "R07.9", "EKG for Chest Pain", ReviewDecision.PAID, null);

        assertNotNull(result);
        assertTrue(result.isValid());
        assertEquals(ReviewDecision.PAID, result.getValidatedDecision());
        assertNotNull(result.getGeneratedCoT());
        assertTrue(result.getGeneratedCoT().contains("PAID"));
        verify(exemplarRepository).save(any(RcmExemplar.class));
    }

    @Test
    void testGenerateAndValidateCoTDeniedOutcome() {
        CoTValidationResult result = generator.generateAndValidateCoT(
                "UHC", "73721", "M25.561", "MRI Knee", ReviewDecision.DENIED, "CO-197");

        assertNotNull(result);
        assertTrue(result.isValid());
        assertEquals(ReviewDecision.DENIED, result.getValidatedDecision());
        assertNotNull(result.getGeneratedCoT());
        assertTrue(result.getGeneratedCoT().contains("DENIED") || result.getGeneratedCoT().contains("CO-197"));
        verify(exemplarRepository).save(any(RcmExemplar.class));
    }
}
