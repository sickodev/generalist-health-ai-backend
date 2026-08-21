package com.generalisthealthai.rcm.infrastructure.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.generalisthealthai.rcm.api.dto.AuditReportResponseDto;
import com.generalisthealthai.rcm.domain.enums.DenialRisk;
import com.generalisthealthai.rcm.domain.enums.ReviewDecision;
import com.generalisthealthai.rcm.domain.enums.JobStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedisJobCacheServiceTest {

    private RedisJobCacheService cacheService;

    @BeforeEach
    void setUp() {
        // Test fallback in-memory cache without requiring external Redis instance
        cacheService = new RedisJobCacheService(Optional.empty(), new ObjectMapper());
    }

    @Test
    void testCacheAndGetJobStatus() {
        UUID jobId = UUID.randomUUID();

        cacheService.cacheJobStatus(jobId, JobStatus.PROCESSING, Duration.ofMinutes(5));

        Optional<JobStatus> status = cacheService.getJobStatus(jobId);
        assertTrue(status.isPresent());
        assertEquals(JobStatus.PROCESSING, status.get());
    }

    @Test
    void testCacheAndGetAuditReport() {
        UUID jobId = UUID.randomUUID();
        AuditReportResponseDto reportDto = AuditReportResponseDto.builder()
                .jobId(jobId)
                .decision(ReviewDecision.PAID)
                .denialRisk(DenialRisk.LOW_RISK)
                .riskScore(0.05)
                .auditRationale("Valid claim.")
                .build();

        cacheService.cacheAuditReport(jobId, reportDto, Duration.ofMinutes(10));

        Optional<AuditReportResponseDto> retrieved = cacheService.getAuditReport(jobId);
        assertTrue(retrieved.isPresent());
        assertEquals(ReviewDecision.PAID, retrieved.get().getDecision());
        assertEquals(0.05, retrieved.get().getRiskScore());
    }

    @Test
    void testEvictJobCache() {
        UUID jobId = UUID.randomUUID();
        cacheService.cacheJobStatus(jobId, JobStatus.DONE, Duration.ofMinutes(5));

        cacheService.evict(jobId);

        Optional<JobStatus> status = cacheService.getJobStatus(jobId);
        assertFalse(status.isPresent());
    }
}
