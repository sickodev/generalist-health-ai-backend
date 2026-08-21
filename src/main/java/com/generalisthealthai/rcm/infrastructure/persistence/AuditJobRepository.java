package com.generalisthealthai.rcm.infrastructure.persistence;

import com.generalisthealthai.rcm.domain.enums.JobStatus;
import com.generalisthealthai.rcm.domain.model.AuditJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditJobRepository extends JpaRepository<AuditJob, UUID> {

    List<AuditJob> findByStatus(JobStatus status);

    List<AuditJob> findByPayerId(String payerId);

    List<AuditJob> findByPatientId(String patientId);

    long countByStatus(JobStatus status);
}
