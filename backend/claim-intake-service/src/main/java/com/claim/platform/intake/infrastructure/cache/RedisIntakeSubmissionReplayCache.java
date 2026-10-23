package com.claim.platform.intake.infrastructure.cache;

import com.claim.platform.intake.application.port.IntakeSubmissionReplayCache;
import com.claim.platform.intake.application.service.IntakeMetricConstants;
import com.claim.platform.intake.infrastructure.config.IntakeSubmissionReplayCacheProperties;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

/**
 * 使用 Redis 实现签收幂等键到签收编号的 Cache-Aside 定位缓存。
 * Redis 异常被视为缓存未命中或无副作用写入，MySQL 仍提供状态读取和最终幂等保证。
 *
 * @author wangguangwu
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisIntakeSubmissionReplayCache implements IntakeSubmissionReplayCache {

    private final StringRedisTemplate redisTemplate;
    private final IntakeSubmissionReplayCacheProperties properties;
    private final MeterRegistry meterRegistry;

    @Override
    public Optional<String> findSignNo(String tenantCode, String externalApplyNo) {
        try {
            String signNo = redisTemplate.opsForValue().get(buildKey(tenantCode, externalApplyNo));
            if (signNo == null || signNo.isBlank()) {
                record(IntakeMetricConstants.OPERATION_READ, IntakeMetricConstants.RESULT_MISS);
                return Optional.empty();
            }
            record(IntakeMetricConstants.OPERATION_READ, IntakeMetricConstants.RESULT_HIT);
            return Optional.of(signNo);
        } catch (RuntimeException exception) {
            record(IntakeMetricConstants.OPERATION_READ, IntakeMetricConstants.RESULT_UNAVAILABLE);
            log.warn("claim intake replay cache read unavailable tenantCode={}", tenantCode, exception);
            return Optional.empty();
        }
    }

    @Override
    public void put(String tenantCode, String externalApplyNo, String signNo) {
        try {
            redisTemplate.opsForValue().set(buildKey(tenantCode, externalApplyNo), signNo, properties.getTtl());
            record(IntakeMetricConstants.OPERATION_WRITE, IntakeMetricConstants.RESULT_SUCCESS);
        } catch (RuntimeException exception) {
            record(IntakeMetricConstants.OPERATION_WRITE, IntakeMetricConstants.RESULT_UNAVAILABLE);
            log.warn("claim intake replay cache write unavailable tenantCode={}", tenantCode, exception);
        }
    }

    @Override
    public void evict(String tenantCode, String externalApplyNo) {
        try {
            redisTemplate.delete(buildKey(tenantCode, externalApplyNo));
            record(IntakeMetricConstants.OPERATION_EVICT, IntakeMetricConstants.RESULT_SUCCESS);
        } catch (RuntimeException exception) {
            record(IntakeMetricConstants.OPERATION_EVICT, IntakeMetricConstants.RESULT_UNAVAILABLE);
            log.warn("claim intake replay cache eviction unavailable tenantCode={}", tenantCode, exception);
        }
    }

    private String buildKey(String tenantCode, String externalApplyNo) {
        return properties.getKeyPrefix()
                + sha256(tenantCode + RedisCacheConstants.KEY_SEPARATOR + externalApplyNo);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance(RedisCacheConstants.SHA_256);
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 摘要算法不可用", exception);
        }
    }

    private void record(String operation, String result) {
        meterRegistry.counter(IntakeMetricConstants.REPLAY_CACHE_METRIC,
                IntakeMetricConstants.TAG_OPERATION, operation,
                IntakeMetricConstants.TAG_RESULT, result).increment();
    }
}
