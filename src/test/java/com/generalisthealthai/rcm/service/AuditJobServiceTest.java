package com.generalisthealthai.rcm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.generalisthealthai.rcm.api.dto.AuditJobStatusResponseDto;
import com.generalisthealthai.rcm.domain.enums.AuditType;
import com.generalisthealthai.rcm.domain.enums.DenialRisk;
import com.generalisthealthai.rcm.domain.enums.JobStatus;
import com.generalisthealthai.rcm.domain.enums.ReviewDecision;
import com.generalisthealthai.rcm.domain.model.AuditJob;
import com.generalisthealthai.rcm.domain.model.AuditReport;
import com.generalisthealthai.rcm.domain.model.ClaimMetadata;
import com.generalisthealthai.rcm.domain.model.EligibilityRecord;
import com.generalisthealthai.rcm.infrastructure.cache.RedisJobCacheService;
import com.generalisthealthai.rcm.infrastructure.persistence.AuditJobRepository;
import com.generalisthealthai.rcm.infrastructure.persistence.AuditReportRepository;
import com.generalisthealthai.rcm.ingestion.dto.ClaimIngestRequestDto;
import com.generalisthealthai.rcm.ingestion.ingestor.ClaimMetadataIngestor;
import com.generalisthealthai.rcm.ingestion.parser.Edi271Parser;
import com.generalisthealthai.rcm.medprompt.ensemble.EnsembleOrchestrator;
import com.generalisthealthai.rcm.medprompt.ensemble.dto.EnsembleConsensusResult;
import com.generalisthealthai.rcm.medprompt.llm.LlmRouter;
import com.generalisthealthai.rcm.medprompt.retrieval.VectorSearchService;
import com.generalisthealthai.rcm.output.AuditReportGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditJobServiceTest {

    private AuditJobRepository jobRepository;
    private AuditReportRepository reportRepository;
    private RedisJobCacheService cacheService;
    private Edi271Parser edi271Parser;
    private ClaimMetadataIngestor claimIngestor;
    private VectorSearchService vectorSearchService;
    private EnsembleOrchestrator ensembleOrchestrator;
    private LlmRouter llmRouter;
    private AuditReportGenerator reportGenerator;
    private AuditJobService auditJobService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        jobRepository = Mockito.mock(AuditJobRepository.class);
        reportRepository = Mockito.mock(AuditReportRepository.class);
        cacheService = new RedisJobCacheService(Optional.empty(), new ObjectMapper());
        edi271Parser = new Edi271Parser(new ObjectMapper());
        claimIngestor = new ClaimMetadataIngestor();
        vectorSearchService = Mockito.mock(VectorSearchService.class);
        ensembleOrchestrator = Mockito.mock(EnsembleOrchestrator.class);
        llmRouter = Mockito.mock(LlmRouter.class);
        reportGenerator = Mockito.mock(AuditReportGenerator.class);
        objectMapper = new ObjectMapper();

        auditJobService = new AuditJobService(
                jobRepository,
                reportRepository,
                cacheService,
                edi271Parser,
                claimIngestor,
                vectorSearchService,
                ensembleOrchestrator,
                llmRouter,
                reportGenerator,
                objectMapper
        );
    }

    @Test
    void testSubmitClaimAuditJob() {
        ClaimIngestRequestDto requestDto = ClaimIngestRequestDto.builder()
                .payerId("BCBS")
                .cptCodes(List.of("93000"))
                .primaryIcd10("R07.9")
                .build();

        UUID generatedId = UUID.randomUUID();
        when(jobRepository.save(any(AuditJob.class))).thenAnswer(inv -> {
            AuditJob j = inv.getArgument(0);
            if (j.getId() == null) {
                j.setId(generatedId);
            }
            return j;
        });

        AuditJob job = auditJobService.submitClaimAuditJob(requestDto);

        assertNotNull(job);
        assertEquals(AuditType.CLAIM_DENIAL_PREDICTION, job.getAuditType());
        assertEquals("BCBS", job.getPayerId());
        verify(jobRepository, Mockito.atLeastOnce()).save(any(AuditJob.class));
    }

    @Test
    void testProcessJobAsyncSuccess() throws Exception {
        UUID jobId = UUID.randomUUID();
        ClaimIngestRequestDto requestDto = ClaimIngestRequestDto.builder()
                .payerId("BCBS")
                .cptCodes(List.of("93000"))
                .primaryIcd10("R07.9")
                .build();

        AuditJob initialJob = AuditJob.builder()
                .id(jobId)
                .status(JobStatus.PENDING)
                .auditType(AuditType.CLAIM_DENIAL_PREDICTION)
                .payerId("BCBS")
                .rawPayload(objectMapper.writeValueAsString(requestDto))
                .build();

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(initialJob));
        when(jobRepository.save(any(AuditJob.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vectorSearchService.findTopKExemplarsForText(anyString(), anyInt())).thenReturn(List.of());

        EnsembleConsensusResult mockConsensus = EnsembleConsensusResult.builder()
                .consensusDecision(ReviewDecision.PAID)
                .consensusDenialRisk(DenialRisk.LOW_RISK)
                .averageRiskScore(0.05)
                .primaryRationale("Chest pain validates EKG.")
                .build();

        when(ensembleOrchestrator.executeEnsemble(any(), any(), any(), any())).thenReturn(mockConsensus);

        AuditReport mockReport = AuditReport.builder()
                .id(UUID.randomUUID())
                .jobId(jobId)
                .decision(ReviewDecision.PAID)
                .denialRisk(DenialRisk.LOW_RISK)
                .riskScore(0.05)
                .auditRationale("Chest pain validates EKG.")
                .build();

        when(reportGenerator.generateReport(any(), any(), any())).thenReturn(mockReport);
        when(reportRepository.save(any(AuditReport.class))).thenReturn(mockReport);

        auditJobService.processJobAsync(jobId);

        assertEquals(JobStatus.DONE, initialJob.getStatus());
        assertNotNull(initialJob.getCompletedAt());
        verify(reportRepository).save(any(AuditReport.class));
    }

    @Test
    void testGetJobStatus() {
        UUID jobId = UUID.randomUUID();
        AuditJob job = AuditJob.builder()
                .id(jobId)
                .status(JobStatus.DONE)
                .auditType(AuditType.CLAIM_DENIAL_PREDICTION)
                .payerId("BCBS")
                .build();

        AuditReport report = AuditReport.builder()
                .id(UUID.randomUUID())
                .jobId(jobId)
                .decision(ReviewDecision.PAID)
                .denialRisk(DenialRisk.LOW_RISK)
                .riskScore(0.05)
                .build();

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(reportRepository.findByJobId(jobId)).thenReturn(Optional.of(report));

        Optional<AuditJobStatusResponseDto> response = auditJobService.getJobStatus(jobId);

        assertTrue(response.isPresent());
        assertEquals(JobStatus.DONE, response.get().getStatus());
        assertNotNull(response.get().getReport());
        assertEquals(ReviewDecision.PAID, response.get().getReport().getDecision());
    }
}
