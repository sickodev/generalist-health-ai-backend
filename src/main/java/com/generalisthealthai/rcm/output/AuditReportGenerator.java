package com.generalisthealthai.rcm.output;

import com.generalisthealthai.rcm.domain.enums.DenialRisk;
import com.generalisthealthai.rcm.domain.enums.ReviewDecision;
import com.generalisthealthai.rcm.domain.model.AuditReport;
import com.generalisthealthai.rcm.domain.model.ClaimMetadata;
import com.generalisthealthai.rcm.medprompt.ensemble.dto.EnsembleConsensusResult;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AuditReportGenerator {

    private final AppealLetterGenerator appealLetterGenerator;

    public AuditReportGenerator(AppealLetterGenerator appealLetterGenerator) {
        this.appealLetterGenerator = appealLetterGenerator;
    }

    /**
     * Synthesizes an AuditReport entity from the ensemble consensus result.
     */
    public AuditReport generateReport(UUID jobId, ClaimMetadata claim, EnsembleConsensusResult consensus) {
        String appealDraft = consensus.getAppealLetterDraft();

        // If high risk or denied and appeal draft is missing, generate a formal appeal letter
        if ((appealDraft == null || appealDraft.isBlank())
                && (consensus.getConsensusDecision() == ReviewDecision.DENIED
                || consensus.getConsensusDenialRisk() == DenialRisk.HIGH_RISK)) {
            appealDraft = appealLetterGenerator.generateAppealLetter(
                    claim,
                    consensus.getConsensusDenialCodes(),
                    consensus.getPrimaryRationale(),
                    null
            );
        }

        return AuditReport.builder()
                .jobId(jobId)
                .decision(consensus.getConsensusDecision())
                .denialRisk(consensus.getConsensusDenialRisk())
                .riskScore(consensus.getAverageRiskScore())
                .predictedDenialCodes(consensus.getConsensusDenialCodes())
                .auditRationale(consensus.getPrimaryRationale())
                .appealLetterDraft(appealDraft)
                .confidenceScore(consensus.getAgreementConfidenceScore())
                .patientResponsibilityAmount(consensus.getAveragePatientResponsibility())
                .build();
    }
}
