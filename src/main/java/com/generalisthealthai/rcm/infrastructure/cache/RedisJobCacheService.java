package com.generalisthealthai.rcm.infrastructure.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.generalisthealthai.rcm.api.dto.AuditReportResponseDto;
import com.generalisthealthai.rcm.domain.enums.JobStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RedisJobCacheService {

    private static final Logger log = LoggerFactory.getLogger(RedisJobCacheService.class);
    private static final String STATUS_PREFIX = "rcm:job:status:";
    private static final String REPORT_PREFIX = "rcm:job:report:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    // In-memory fallback map for offline/embedded mode
    private final Map<String, CacheEntry<String>> localCache = new ConcurrentHashMap<>();

    public RedisJobCacheService(
            Optional<StringRedisTemplate> redisTemplateOptional,
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplateOptional.orElse(null);
        this.objectMapper = objectMapper;
    }

    public void cacheJobStatus(UUID jobId, JobStatus status, Duration ttl) {
        String key = STATUS_PREFIX + jobId;
        String val = status.name();

        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(key, val, ttl);
                return;
            } catch (Exception e) {
                log.debug("Redis set failed ({}); using local cache fallback", e.getMessage());
            }
        }
        localCache.put(key, new CacheEntry<>(val, Instant.now().plus(ttl)));
    }

    public Optional<JobStatus> getJobStatus(UUID jobId) {
        String key = STATUS_PREFIX + jobId;

        if (redisTemplate != null) {
            try {
                String val = redisTemplate.opsForValue().get(key);
                if (val != null && !val.isBlank()) {
                    return Optional.of(JobStatus.valueOf(val.trim()));
                }
            } catch (Exception e) {
                log.debug("Redis get failed ({}); using local cache fallback", e.getMessage());
            }
        }

        CacheEntry<String> entry = localCache.get(key);
        if (entry != null && !entry.isExpired()) {
            return Optional.of(JobStatus.valueOf(entry.value));
        }
        return Optional.empty();
    }

    public void cacheAuditReport(UUID jobId, AuditReportResponseDto reportDto, Duration ttl) {
        String key = REPORT_PREFIX + jobId;
        try {
            String json = objectMapper.writeValueAsString(reportDto);
            if (redisTemplate != null) {
                try {
                    redisTemplate.opsForValue().set(key, json, ttl);
                    return;
                } catch (Exception e) {
                    log.debug("Redis report cache failed ({}); using local cache fallback", e.getMessage());
                }
            }
            localCache.put(key, new CacheEntry<>(json, Instant.now().plus(ttl)));
        } catch (Exception e) {
            log.error("Failed to serialize AuditReportResponseDto for caching: {}", e.getMessage());
        }
    }

    public Optional<AuditReportResponseDto> getAuditReport(UUID jobId) {
        String key = REPORT_PREFIX + jobId;
        String json = null;

        if (redisTemplate != null) {
            try {
                json = redisTemplate.opsForValue().get(key);
            } catch (Exception e) {
                log.debug("Redis report get failed ({}); using local cache fallback", e.getMessage());
            }
        }

        if (json == null) {
            CacheEntry<String> entry = localCache.get(key);
            if (entry != null && !entry.isExpired()) {
                json = entry.value;
            }
        }

        if (json != null && !json.isBlank()) {
            try {
                return Optional.of(objectMapper.readValue(json, AuditReportResponseDto.class));
            } catch (Exception e) {
                log.error("Failed to deserialize cached AuditReportResponseDto: {}", e.getMessage());
            }
        }

        return Optional.empty();
    }

    public void evict(UUID jobId) {
        String statusKey = STATUS_PREFIX + jobId;
        String reportKey = REPORT_PREFIX + jobId;

        if (redisTemplate != null) {
            try {
                redisTemplate.delete(statusKey);
                redisTemplate.delete(reportKey);
            } catch (Exception e) {
                log.debug("Redis delete failed ({}); evicting local cache", e.getMessage());
            }
        }
        localCache.remove(statusKey);
        localCache.remove(reportKey);
    }

    private static class CacheEntry<T> {
        final T value;
        final Instant expiresAt;

        CacheEntry(T value, Instant expiresAt) {
            this.value = value;
            this.expiresAt = expiresAt;
        }

        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
