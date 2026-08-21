package com.generalisthealthai.rcm.infrastructure.persistence;

import com.generalisthealthai.rcm.domain.enums.AuditType;
import com.generalisthealthai.rcm.domain.enums.DenialRisk;
import com.generalisthealthai.rcm.domain.enums.JobStatus;
import com.generalisthealthai.rcm.domain.enums.ReviewDecision;
import com.generalisthealthai.rcm.domain.model.AuditJob;
import com.generalisthealthai.rcm.domain.model.AuditReport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("dev")
class AuditReportRepositoryTest {

    @Autowired
    private AuditJobRepository auditJobRepository;

    @Autowired
    private AuditReportRepository auditReportRepository;

    @Test
    void testSaveAndRetrieveAuditReport() {
        AuditJob job = auditJobRepository.save(AuditJob.builder()
                .status(JobStatus.DONE)
                .auditType(AuditType.CLAIM_DENIAL_PREDICTION)
                .payerId("AETNA-01")
                .patientId("PAT-2001")
                .build());

        AuditReport report = AuditReport.builder()
                .jobId(job.getId())
                .decision(ReviewDecision.DENIED)
                .denialRisk(DenialRisk.HIGH_RISK)
                .riskScore(0.88)
                .predictedDenialCodes(List.of("CO-16", "CO-50"))
                .auditRationale("Claim lacked required prior authorization.")
                .appealLetterDraft("To whom it may concern...")
                .confidenceScore(0.92)
                .patientResponsibilityAmount(new BigDecimal("150.00"))
                .build();

        AuditReport saved = auditReportRepository.save(report);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());

        Optional<AuditReport> found = auditReportRepository.findByJobId(job.getId());
        assertTrue(found.isPresent());
        assertEquals(ReviewDecision.DENIED, found.get().getDecision());
        assertEquals(DenialRisk.HIGH_RISK, found.get().getDenialRisk());
        assertEquals(2, found.get().getPredictedDenialCodes().size());
        assertTrue(auditReportRepository.existsByJobId(job.getId()));
    }
}
