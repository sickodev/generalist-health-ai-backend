package com.generalisthealthai.rcm.medprompt.seed;

import com.generalisthealthai.rcm.infrastructure.persistence.RcmExemplarRepository;
import com.generalisthealthai.rcm.medprompt.embedding.EmbeddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExemplarSeedServiceTest {

    private RcmExemplarRepository exemplarRepository;
    private EmbeddingService embeddingService;
    private ExemplarSeedService seedService;

    @BeforeEach
    void setUp() {
        exemplarRepository = Mockito.mock(RcmExemplarRepository.class);
        embeddingService = Mockito.mock(EmbeddingService.class);
        when(embeddingService.embed(anyString())).thenReturn(new float[768]);
        seedService = new ExemplarSeedService(exemplarRepository, embeddingService);
    }

    @Test
    void testSeedsWhenRepositoryIsEmpty() {
        when(exemplarRepository.count()).thenReturn(0L);

        seedService.seedExemplarsIfEmpty();

        verify(exemplarRepository).saveAll(anyList());
    }

    @Test
    void testDoesNotSeedWhenRepositoryAlreadyHasData() {
        when(exemplarRepository.count()).thenReturn(6L);

        seedService.seedExemplarsIfEmpty();

        verify(exemplarRepository, never()).saveAll(anyList());
    }
}
