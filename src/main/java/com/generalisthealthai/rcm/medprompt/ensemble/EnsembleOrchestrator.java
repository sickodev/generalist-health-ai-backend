package com.generalisthealthai.rcm.medprompt.ensemble;

import com.generalisthealthai.rcm.domain.model.ClaimMetadata;
import com.generalisthealthai.rcm.domain.model.EligibilityRecord;
import com.generalisthealthai.rcm.domain.model.RcmExemplar;
import com.generalisthealthai.rcm.medprompt.ensemble.dto.EnsembleConsensusResult;
import com.generalisthealthai.rcm.medprompt.ensemble.dto.LlmAuditResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class EnsembleOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(EnsembleOrchestrator.class);

    private final InputShuffler inputShuffler;
    private final ConsensusVoter consensusVoter;
    private final Executor executor;
    private final int ensembleSize;
    private final long timeoutMs;

    public EnsembleOrchestrator(
            InputShuffler inputShuffler,
            ConsensusVoter consensusVoter,
            @Qualifier("taskExecutor") Executor executor,
            @Value("${rcm.medprompt.ensemble-size:4}") int ensembleSize,
            @Value("${rcm.medprompt.ensemble-timeout-ms:8000}") long timeoutMs) {
        this.inputShuffler = inputShuffler;
        this.consensusVoter = consensusVoter;
        this.executor = executor;
        this.ensembleSize = Math.max(1, ensembleSize);
        this.timeoutMs = timeoutMs;
    }

    /**
     * Executes parallel Medprompt ensemble runs with shuffled input variants and aggregates consensus.
     */
    public EnsembleConsensusResult executeEnsemble(
            ClaimMetadata claim,
            EligibilityRecord eligibility,
            List<RcmExemplar> exemplars,
            Function<String, LlmAuditResponseDto> inferenceRunner) {

        List<String> promptVariants = inputShuffler.generatePromptVariants(claim, eligibility, exemplars, ensembleSize);
        log.debug("Generated {} prompt variants for ensemble execution", promptVariants.size());

        List<CompletableFuture<LlmAuditResponseDto>> futures = new ArrayList<>(promptVariants.size());

        for (int i = 0; i < promptVariants.size(); i++) {
            final int variantIdx = i;
            final String prompt = promptVariants.get(i);

            CompletableFuture<LlmAuditResponseDto> future = CompletableFuture.supplyAsync(() -> {
                try {
                    LlmAuditResponseDto response = inferenceRunner.apply(prompt);
                    if (response != null) {
                        response.setVariantIndex(variantIdx);
                    }
                    return response;
                } catch (Exception e) {
                    log.warn("Ensemble run for variant {} failed: {}", variantIdx, e.getMessage());
                    return null;
                }
            }, executor);

            futures.add(future);
        }

        // Await all futures with timeout limit
        try {
            CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            allOf.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.warn("Ensemble parallel execution timed out or partially completed ({}ms SLA): {}", timeoutMs, e.getMessage());
        }

        // Collect all completed non-null results
        List<LlmAuditResponseDto> completedRuns = futures.stream()
                .filter(f -> f.isDone() && !f.isCompletedExceptionally())
                .map(f -> {
                    try {
                        return f.getNow(null);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        log.info("Ensemble completed {}/{} successful runs for claim {}", completedRuns.size(), promptVariants.size(), claim.getClaimId());

        return consensusVoter.vote(completedRuns);
    }
}
