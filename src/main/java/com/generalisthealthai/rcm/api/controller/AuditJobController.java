package com.generalisthealthai.rcm.api.controller;

import com.generalisthealthai.rcm.api.dto.AppealDraftRequestDto;
import com.generalisthealthai.rcm.api.dto.AuditJobStatusResponseDto;
import com.generalisthealthai.rcm.api.dto.AuditReportResponseDto;
import com.generalisthealthai.rcm.common.exception.ResourceNotFoundException;
import com.generalisthealthai.rcm.domain.enums.JobStatus;
import com.generalisthealthai.rcm.domain.model.AuditJob;
import com.generalisthealthai.rcm.domain.model.ClaimMetadata;
import com.generalisthealthai.rcm.domain.model.RcmExemplar;
import com.generalisthealthai.rcm.infrastructure.persistence.AuditJobRepository;
import com.generalisthealthai.rcm.ingestion.dto.ClaimIngestRequestDto;
import com.generalisthealthai.rcm.ingestion.ingestor.ClaimMetadataIngestor;
import com.generalisthealthai.rcm.medprompt.ensemble.EnsembleOrchestrator;
import com.generalisthealthai.rcm.medprompt.ensemble.dto.EnsembleConsensusResult;
import com.generalisthealthai.rcm.medprompt.llm.LlmRouter;
import com.generalisthealthai.rcm.medprompt.retrieval.VectorSearchService;
import com.generalisthealthai.rcm.output.AppealLetterGenerator;
import com.generalisthealthai.rcm.output.AuditReportGenerator;
import com.generalisthealthai.rcm.service.AuditJobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/rcm")
@Tag(name = "RCM Audit & Prior Authorization", description = "Endpoints for AI-driven revenue cycle claim auditing and PA verification")
public class AuditJobController {

    private final AuditJobService auditJobService;
    private final AuditJobRepository auditJobRepository;
    private final ClaimMetadataIngestor claimIngestor;
    private final VectorSearchService vectorSearchService;
    private final EnsembleOrchestrator ensembleOrchestrator;
    private final LlmRouter llmRouter;
    private final AuditReportGenerator reportGenerator;
    private final AppealLetterGenerator appealLetterGenerator;

    public AuditJobController(
            AuditJobService auditJobService,
            AuditJobRepository auditJobRepository,
            ClaimMetadataIngestor claimIngestor,
            VectorSearchService vectorSearchService,
            EnsembleOrchestrator ensembleOrchestrator,
            LlmRouter llmRouter,
            AuditReportGenerator reportGenerator,
            AppealLetterGenerator appealLetterGenerator) {
        this.auditJobService = auditJobService;
        this.auditJobRepository = auditJobRepository;
        this.claimIngestor = claimIngestor;
        this.vectorSearchService = vectorSearchService;
        this.ensembleOrchestrator = ensembleOrchestrator;
        this.llmRouter = llmRouter;
        this.reportGenerator = reportGenerator;
        this.appealLetterGenerator = appealLetterGenerator;
    }

    @PostMapping("/audit/claim")
    @Operation(summary = "Submit Claim for Asynchronous Denial Prediction Audit", description = "Submits a standard health claim to the Medprompt ensemble pipeline for async risk assessment.")
    @ApiResponse(responseCode = "202", description = "Claim audit job successfully accepted for asynchronous processing")
    public ResponseEntity<AuditJobStatusResponseDto> submitClaimAudit(@Valid @RequestBody ClaimIngestRequestDto requestDto) {
        AuditJob job = auditJobService.submitClaimAuditJob(requestDto);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(AuditJobStatusResponseDto.builder()
                        .jobId(job.getId())
                        .status(job.getStatus())
                        .auditType(job.getAuditType())
                        .payerId(job.getPayerId())
                        .patientId(job.getPatientId())
                        .createdAt(job.getCreatedAt())
                        .build());
    }

    @PostMapping("/verify/edi271")
    @Operation(summary = "Submit EDI 271 for Asynchronous PA Verification", description = "Submits an ANSI X12 271 Eligibility / Benefit payload for async Prior Authorization verification.")
    @ApiResponse(responseCode = "202", description = "EDI verification job successfully accepted")
    public ResponseEntity<AuditJobStatusResponseDto> submitEdi271Verification(@RequestBody String edi271Payload) {
        AuditJob job = auditJobService.submitVerificationJob(edi271Payload);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(AuditJobStatusResponseDto.builder()
                        .jobId(job.getId())
                        .status(job.getStatus())
                        .auditType(job.getAuditType())
                        .payerId(job.getPayerId())
                        .patientId(job.getPatientId())
                        .createdAt(job.getCreatedAt())
                        .build());
    }

    @GetMapping("/jobs/{jobId}")
    @Operation(summary = "Get Audit Job Status & Results", description = "Polls the status of an audit job and retrieves the completed AuditReport.")
    public ResponseEntity<AuditJobStatusResponseDto> getJobStatus(@PathVariable UUID jobId) {
        return auditJobService.getJobStatus(jobId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Audit job with ID " + jobId + " not found"));
    }

    @GetMapping("/jobs")
    @Operation(summary = "List Recent Audit Jobs", description = "Retrieves recent audit jobs with optional status filtering.")
    public ResponseEntity<List<AuditJobStatusResponseDto>> listJobs(@RequestParam(required = false) JobStatus status) {
        List<AuditJob> jobs = (status != null)
                ? auditJobRepository.findByStatus(status)
                : auditJobRepository.findAll();

        List<AuditJobStatusResponseDto> responseList = jobs.stream()
                .map(job -> AuditJobStatusResponseDto.builder()
                        .jobId(job.getId())
                        .status(job.getStatus())
                        .auditType(job.getAuditType())
                        .payerId(job.getPayerId())
                        .patientId(job.getPatientId())
                        .errorMessage(job.getErrorMessage())
                        .createdAt(job.getCreatedAt())
                        .completedAt(job.getCompletedAt())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(responseList);
    }

    @PostMapping("/audit/claim/sync")
    @Operation(summary = "Synchronous Claim Audit", description = "Executes the full Medprompt audit pipeline synchronously for immediate feedback.")
    public ResponseEntity<AuditReportResponseDto> auditClaimSync(@Valid @RequestBody ClaimIngestRequestDto requestDto) {
        ClaimMetadata claim = claimIngestor.ingest(requestDto);
        String canonicalText = claimIngestor.buildCanonicalEmbeddingText(claim, null);
        List<RcmExemplar> exemplars = vectorSearchService.findTopKExemplarsForText(canonicalText, 4);

        EnsembleConsensusResult consensus = ensembleOrchestrator.executeEnsemble(
                claim,
                null,
                exemplars,
                llmRouter.getInferenceFunction()
        );

        UUID syncJobId = UUID.randomUUID();
        var report = reportGenerator.generateReport(syncJobId, claim, consensus);

        return ResponseEntity.ok(AuditReportResponseDto.builder()
                .reportId(UUID.randomUUID())
                .jobId(syncJobId)
                .decision(report.getDecision())
                .denialRisk(report.getDenialRisk())
                .riskScore(report.getRiskScore())
                .predictedDenialCodes(report.getPredictedDenialCodes())
                .auditRationale(report.getAuditRationale())
                .appealLetterDraft(report.getAppealLetterDraft())
                .confidenceScore(report.getConfidenceScore())
                .patientResponsibilityAmount(report.getPatientResponsibilityAmount())
                .build());
    }

    @PostMapping("/appeal/draft")
    @Operation(summary = "Draft Formal Denial Appeal Letter", description = "Generates a customized, formal insurance appeal letter citing clinical criteria.")
    public ResponseEntity<Map<String, String>> draftAppeal(@Valid @RequestBody AppealDraftRequestDto requestDto) {
        ClaimMetadata claim = claimIngestor.ingest(requestDto.getClaim());
        String letter = appealLetterGenerator.generateAppealLetter(
                claim,
                requestDto.getDenialCodes(),
                requestDto.getAuditRationale(),
                requestDto.getCustomPayerPolicy()
        );
        return ResponseEntity.ok(Map.of("appealLetter", letter));
    }
}
