package com.generalisthealthai.rcm.api.dto;

import com.generalisthealthai.rcm.domain.enums.AuditType;
import com.generalisthealthai.rcm.domain.enums.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditJobStatusResponseDto {

    private UUID jobId;
    private JobStatus status;
    private AuditType auditType;
    private String payerId;
    private String patientId;
    private String errorMessage;
    private Instant createdAt;
    private Instant completedAt;
    private AuditReportResponseDto report;
}
