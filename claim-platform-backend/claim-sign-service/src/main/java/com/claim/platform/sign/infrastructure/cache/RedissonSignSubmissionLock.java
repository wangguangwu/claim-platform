package com.claim.platform.sign.infrastructure.cache;

import com.claim.platform.sign.application.port.SignSubmissionLock;
import com.claim.platform.sign.application.port.SignSubmissionLockHandle;
import com.claim.platform.sign.application.port.LockAcquireStatus;
import com.claim.platform.sign.application.service.SignMetricConstants;
import com.claim.platform.sign.infrastructure.config.SignSubmissionLockProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;

/**
 * 使用 Redisson 实现按租户和外部申请编号互斥的短期业务锁。
 * 锁键只保存业务键摘要；持有期间由 Redisson watchdog 自动续期，
 * Redis 不可用时返回降级结果，由 MySQL 唯一键继续保证幂等。
 *
 * @author wangguangwu
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedissonSignSubmissionLock implements SignSubmissionLock {

    private final RedissonClient redissonClient;
    private final SignSubmissionLockProperties properties;
    private final MeterRegistry meterRegistry;

    @Override
    public SignSubmissionLockHandle tryAcquire(String tenantCode, String externalApplyNo) {
        String lockKey = buildLockKey(tenantCode, externalApplyNo);
        long startedAt = System.nanoTime();
        try {
            RLock lock = redissonClient.getLock(lockKey);
            boolean acquired = lock.tryLock(properties.getWaitTime().toMillis(), TimeUnit.MILLISECONDS);
            if (!acquired) {
                recordLockTimer(startedAt, SignMetricConstants.RESULT_TIMEOUT);
                log.warn("claim sign submission lock timeout tenantCode={} result={}", tenantCode,
                        SignMetricConstants.RESULT_TIMEOUT);
                return new StaticHandle(LockAcquireStatus.TIMEOUT);
            }
            recordLockTimer(startedAt, SignMetricConstants.RESULT_ACQUIRED);
            return new AcquiredHandle(lock, tenantCode);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            recordLockTimer(startedAt, SignMetricConstants.RESULT_TIMEOUT);
            log.warn("claim sign submission lock interrupted tenantCode={} result={}", tenantCode,
                    SignMetricConstants.RESULT_TIMEOUT);
            return new StaticHandle(LockAcquireStatus.TIMEOUT);
        } catch (RedisException exception) {
            recordLockTimer(startedAt, SignMetricConstants.RESULT_UNAVAILABLE);
            log.warn("claim sign submission lock unavailable tenantCode={} result={}", tenantCode,
                    SignMetricConstants.RESULT_UNAVAILABLE, exception);
            return new StaticHandle(LockAcquireStatus.UNAVAILABLE);
        }
    }

    private String buildLockKey(String tenantCode, String externalApplyNo) {
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

    private void recordLockTimer(long startedAt, String result) {
        Timer.builder(SignMetricConstants.SUBMISSION_LOCK_METRIC)
                .tag(SignMetricConstants.TAG_RESULT, result)
                .register(meterRegistry)
                .record(Duration.ofNanos(System.nanoTime() - startedAt));
    }

    /**
     * 超时、中断或降级结果不持有锁，释放时不做任何操作。
     */
    private static final class StaticHandle implements SignSubmissionLockHandle {

        private final LockAcquireStatus status;

        private StaticHandle(LockAcquireStatus status) {
            this.status = status;
        }

        @Override
        public LockAcquireStatus status() {
            return status;
        }

        @Override
        public void close() {
        }
    }

    /**
     * 只释放当前线程仍持有的锁，所有权丢失时不强制删除 Redis 键。
     */
    private static final class AcquiredHandle implements SignSubmissionLockHandle {

        private final RLock lock;
        private final String tenantCode;

        private AcquiredHandle(RLock lock, String tenantCode) {
            this.lock = lock;
            this.tenantCode = tenantCode;
        }

        @Override
        public LockAcquireStatus status() {
            return LockAcquireStatus.ACQUIRED;
        }

        @Override
        public void close() {
            try {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                } else {
                    log.warn("claim sign submission lock ownership lost tenantCode={}", tenantCode);
                }
            } catch (RedisException | IllegalMonitorStateException exception) {
                log.warn("claim sign submission lock release failed tenantCode={} type={}", tenantCode,
                        exception.getClass().getSimpleName(), exception);
            }
        }
    }
}
