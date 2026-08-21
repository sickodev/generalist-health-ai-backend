package com.generalisthealthai.rcm.infrastructure.persistence;

import com.generalisthealthai.rcm.domain.enums.AuditType;
import com.generalisthealthai.rcm.domain.enums.JobStatus;
import com.generalisthealthai.rcm.domain.model.AuditJob;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("dev")
class AuditJobRepositoryTest {

    @Autowired
    private AuditJobRepository auditJobRepository;

    @Test
    void testSaveAndRetrieveAuditJob() {
        AuditJob job = AuditJob.builder()
                .status(JobStatus.PENDING)
                .auditType(AuditType.CLAIM_DENIAL_PREDICTION)
                .payerId("BCBS-001")
                .patientId("PAT-1002")
                .cptCodes(List.of("99214", "93000"))
                .icd10Codes(List.of("R07.9"))
                .rawPayload("{\"test\":\"payload\"}")
                .build();

        AuditJob saved = auditJobRepository.save(job);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());

        Optional<AuditJob> found = auditJobRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("BCBS-001", found.get().getPayerId());
        assertEquals(2, found.get().getCptCodes().size());
        assertEquals("99214", found.get().getCptCodes().get(0));

        List<AuditJob> pendingJobs = auditJobRepository.findByStatus(JobStatus.PENDING);
        assertEquals(1, pendingJobs.size());
        assertEquals(1, auditJobRepository.countByStatus(JobStatus.PENDING));
    }
}
