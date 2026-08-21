package com.generalisthealthai.rcm.infrastructure.persistence;

import com.generalisthealthai.rcm.domain.enums.DenialRisk;
import com.generalisthealthai.rcm.domain.enums.ReviewDecision;
import com.generalisthealthai.rcm.domain.model.AuditReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuditReportRepository extends JpaRepository<AuditReport, UUID> {

    Optional<AuditReport> findByJobId(UUID jobId);

    List<AuditReport> findByDecision(ReviewDecision decision);

    List<AuditReport> findByDenialRisk(DenialRisk denialRisk);

    boolean existsByJobId(UUID jobId);
}
