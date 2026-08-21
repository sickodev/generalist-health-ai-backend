package com.generalisthealthai.rcm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.generalisthealthai.rcm.api.dto.AuditJobStatusResponseDto;
import com.generalisthealthai.rcm.api.dto.AuditReportResponseDto;
import com.generalisthealthai.rcm.domain.enums.AuditType;
import com.generalisthealthai.rcm.domain.enums.JobStatus;
import com.generalisthealthai.rcm.domain.model.AuditJob;
import com.generalisthealthai.rcm.domain.model.AuditReport;
import com.generalisthealthai.rcm.domain.model.ClaimMetadata;
import com.generalisthealthai.rcm.domain.model.EligibilityRecord;
import com.generalisthealthai.rcm.domain.model.RcmExemplar;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuditJobService {

    private static final Logger log = LoggerFactory.getLogger(AuditJobService.class);
    private static final Duration JOB_STATUS_TTL = Duration.ofHours(2);
    private static final Duration AUDIT_REPORT_TTL = Duration.ofHours(24);

    private final AuditJobRepository jobRepository;
    private final AuditReportRepository reportRepository;
    private final RedisJobCacheService cacheService;
    private final Edi271Parser edi271Parser;
    private final ClaimMetadataIngestor claimIngestor;
    private final VectorSearchService vectorSearchService;
    private final EnsembleOrchestrator ensembleOrchestrator;
    private final LlmRouter llmRouter;
    private final AuditReportGenerator reportGenerator;
    private final ObjectMapper objectMapper;

    public AuditJobService(
            AuditJobRepository jobRepository,
            AuditReportRepository reportRepository,
            RedisJobCacheService cacheService,
            Edi271Parser edi271Parser,
            ClaimMetadataIngestor claimIngestor,
            VectorSearchService vectorSearchService,
            EnsembleOrchestrator ensembleOrchestrator,
            LlmRouter llmRouter,
            AuditReportGenerator reportGenerator,
            ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.reportRepository = reportRepository;
        this.cacheService = cacheService;
        this.edi271Parser = edi271Parser;
        this.claimIngestor = claimIngestor;
        this.vectorSearchService = vectorSearchService;
        this.ensembleOrchestrator = ensembleOrchestrator;
        this.llmRouter = llmRouter;
        this.reportGenerator = reportGenerator;
        this.objectMapper = objectMapper;
    }

    /**
     * Submits an EDI 271 payload for asynchronous Prior Authorization Verification.
     */
    @Transactional
    public AuditJob submitVerificationJob(String edi271Payload) {
        EligibilityRecord eligibility = edi271Parser.parse(edi271Payload);

        AuditJob job = AuditJob.builder()
                .status(JobStatus.PENDING)
                .auditType(AuditType.PRIOR_AUTH_VERIFICATION)
                .payerId(eligibility.getPayerId())
                .patientId(eligibility.getPatientId())
                .cptCodes(eligibility.getServiceTypeCodes())
                .rawPayload(edi271Payload)
                .build();

        AuditJob saved = jobRepository.save(job);
        cacheService.cacheJobStatus(saved.getId(), JobStatus.PENDING, JOB_STATUS_TTL);
        log.info("Submitted Prior Authorization Verification Job {}", saved.getId());

        // Trigger asynchronous background processing
        processJobAsync(saved.getId());
        return saved;
    }

    /**
     * Submits claim metadata for asynchronous Claim Denial Prediction Audit.
     */
    @Transactional
    public AuditJob submitClaimAuditJob(ClaimIngestRequestDto requestDto) {
        ClaimMetadata claim = claimIngestor.ingest(requestDto);

        List<String> allIcd10s = new ArrayList<>();
        if (claim.getPrimaryIcd10() != null) {
            allIcd10s.add(claim.getPrimaryIcd10());
        }
        if (claim.getSecondaryIcd10s() != null) {
            allIcd10s.addAll(claim.getSecondaryIcd10s());
        }

        String rawJson = "";
        try {
            rawJson = objectMapper.writeValueAsString(requestDto);
        } catch (Exception e) {
            log.warn("Could not serialize request DTO to JSON: {}", e.getMessage());
        }

        AuditJob job = AuditJob.builder()
                .status(JobStatus.PENDING)
                .auditType(AuditType.CLAIM_DENIAL_PREDICTION)
                .payerId(claim.getPayerId())
                .patientId(claim.getPatientId())
                .cptCodes(claim.getCptCodes())
                .icd10Codes(allIcd10s)
                .rawPayload(rawJson)
                .build();

        AuditJob saved = jobRepository.save(job);
        cacheService.cacheJobStatus(saved.getId(), JobStatus.PENDING, JOB_STATUS_TTL);
        log.info("Submitted Claim Denial Audit Job {}", saved.getId());

        // Trigger asynchronous background processing
        processJobAsync(saved.getId());
        return saved;
    }

    /**
     * Background asynchronous worker executing the Medprompt pipeline.
     */
    @Async("taskExecutor")
    public void processJobAsync(UUID jobId) {
        log.info("Starting async execution of audit job {}", jobId);

        Optional<AuditJob> jobOpt = jobRepository.findById(jobId);
        if (jobOpt.isEmpty()) {
            log.error("Audit job {} not found for processing", jobId);
            return;
        }

        AuditJob job = jobOpt.get();
        job.setStatus(JobStatus.PROCESSING);
        jobRepository.save(job);
        cacheService.cacheJobStatus(jobId, JobStatus.PROCESSING, JOB_STATUS_TTL);

        try {
            ClaimMetadata claim;
            EligibilityRecord eligibility = null;

            if (job.getAuditType() == AuditType.PRIOR_AUTH_VERIFICATION) {
                eligibility = edi271Parser.parse(job.getRawPayload());
                claim = ClaimMetadata.builder()
                        .claimId("VERIF-" + jobId.toString().substring(0, 8))
                        .payerId(eligibility.getPayerId())
                        .patientId(eligibility.getPatientId())
                        .dateOfService(eligibility.getDateOfService())
                        .cptCodes(job.getCptCodes() != null ? job.getCptCodes() : List.of("30"))
                        .primaryIcd10("Z00.00")
                        .build();
            } else {
                ClaimIngestRequestDto dto = objectMapper.readValue(job.getRawPayload(), ClaimIngestRequestDto.class);
                claim = claimIngestor.ingest(dto);
            }

            // 1. Build canonical search query & retrieve top-k exemplars
            String canonicalText = claimIngestor.buildCanonicalEmbeddingText(claim, eligibility);
            List<RcmExemplar> exemplars = vectorSearchService.findTopKExemplarsForText(canonicalText, 4);

            // 2. Execute parallel Medprompt ensemble with input shuffling & consensus voting
            EnsembleConsensusResult consensusResult = ensembleOrchestrator.executeEnsemble(
                    claim,
                    eligibility,
                    exemplars,
                    llmRouter.getInferenceFunction()
            );

            // 3. Generate AuditReport & persist
            AuditReport report = reportGenerator.generateReport(jobId, claim, consensusResult);
            AuditReport savedReport = reportRepository.save(report);

            // 4. Update AuditJob to DONE
            job.setStatus(JobStatus.DONE);
            job.setCompletedAt(Instant.now());
            jobRepository.save(job);

            // 5. Update Redis cache
            cacheService.cacheJobStatus(jobId, JobStatus.DONE, JOB_STATUS_TTL);
            AuditReportResponseDto reportDto = mapToReportDto(savedReport);
            cacheService.cacheAuditReport(jobId, reportDto, AUDIT_REPORT_TTL);

            log.info("Audit job {} completed successfully with decision {}", jobId, savedReport.getDecision());

        } catch (Exception e) {
            log.error("Audit job {} failed: {}", jobId, e.getMessage(), e);
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setCompletedAt(Instant.now());
            jobRepository.save(job);
            cacheService.cacheJobStatus(jobId, JobStatus.FAILED, JOB_STATUS_TTL);
        }
    }

    /**
     * Polls the job status with fast-path Redis caching and DB fallback.
     */
    @Transactional(readOnly = true)
    public Optional<AuditJobStatusResponseDto> getJobStatus(UUID jobId) {
        Optional<AuditReportResponseDto> cachedReport = cacheService.getAuditReport(jobId);
        Optional<JobStatus> cachedStatus = cacheService.getJobStatus(jobId);

        if (cachedStatus.isPresent() && cachedReport.isPresent()) {
            return Optional.of(AuditJobStatusResponseDto.builder()
                    .jobId(jobId)
                    .status(cachedStatus.get())
                    .report(cachedReport.get())
                    .build());
        }

        Optional<AuditJob> jobOpt = jobRepository.findById(jobId);
        if (jobOpt.isEmpty()) {
            return Optional.empty();
        }

        AuditJob job = jobOpt.get();
        AuditReportResponseDto reportDto = reportRepository.findByJobId(jobId)
                .map(this::mapToReportDto)
                .orElse(null);

        return Optional.of(AuditJobStatusResponseDto.builder()
                .jobId(job.getId())
                .status(job.getStatus())
                .auditType(job.getAuditType())
                .payerId(job.getPayerId())
                .patientId(job.getPatientId())
                .errorMessage(job.getErrorMessage())
                .createdAt(job.getCreatedAt())
                .completedAt(job.getCompletedAt())
                .report(reportDto)
                .build());
    }

    private AuditReportResponseDto mapToReportDto(AuditReport report) {
        return AuditReportResponseDto.builder()
                .reportId(report.getId())
                .jobId(report.getJobId())
                .decision(report.getDecision())
                .denialRisk(report.getDenialRisk())
                .riskScore(report.getRiskScore())
                .predictedDenialCodes(report.getPredictedDenialCodes())
                .auditRationale(report.getAuditRationale())
                .appealLetterDraft(report.getAppealLetterDraft())
                .confidenceScore(report.getConfidenceScore())
                .patientResponsibilityAmount(report.getPatientResponsibilityAmount())
                .createdAt(report.getCreatedAt())
                .build();
    }
}
