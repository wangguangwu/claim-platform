package com.claim.platform.sign.infrastructure.cache;

import com.claim.platform.sign.application.port.IssueNumberGenerator;
import com.claim.platform.sign.domain.issue.repository.ClaimIssueRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** Restores and allocates issue numbers per tenant Sign scope through Redis counters.
 *
 * @author wangguangwu
 */
@Component
public class RedisIssueNumberGenerator implements IssueNumberGenerator {
    private final StringRedisTemplate redisTemplate;
    private final ClaimIssueRepository repository;

    public RedisIssueNumberGenerator(StringRedisTemplate redisTemplate, ClaimIssueRepository repository) {
        this.redisTemplate = redisTemplate;
        this.repository = repository;
    }

    /** Initializes a missing counter from persisted facts before allocating the next issue number. */
    @Override
    public String next(String tenantCode, String signNo) {
        String key = "claim:sign:issue:number:" + tenantCode + ':' + signNo;
        if (redisTemplate.opsForValue().get(key) == null) {
            long restored = repository.findHighestIssueNo(tenantCode, signNo).map(this::sequenceOf).orElse(0L);
            redisTemplate.opsForValue().setIfAbsent(key, Long.toString(restored));
        }
        Long sequence = redisTemplate.opsForValue().increment(key);
        if (sequence == null) {
            throw new IllegalStateException("issue number unavailable");
        }
        return "I%06d".formatted(sequence);
    }

    private long sequenceOf(String issueNo) {
        try {
            return Long.parseLong(issueNo.substring(1));
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Unsupported persisted issueNo=" + issueNo, exception);
        }
    }
}
