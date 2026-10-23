package com.claim.platform.intake.infrastructure.cache;

import com.claim.platform.intake.application.port.LockAcquireStatus;
import com.claim.platform.intake.application.port.SignNoRecoveryLock;
import com.claim.platform.intake.application.port.SignNoRecoveryLockHandle;
import com.claim.platform.intake.infrastructure.config.SignNoProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

/**
 * 使用 Redisson 实现按业务日期隔离的签收号恢复锁。
 * 持有期间由现有客户端级 watchdog 续期；等待超时或 Redis 不可用时返回稳定状态，不阻塞请求。
 *
 * @author wangguangwu
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedissonSignNoRecoveryLock implements SignNoRecoveryLock {

    private final RedissonClient redissonClient;
    private final SignNoProperties properties;

    @Override
    public SignNoRecoveryLockHandle tryAcquire(LocalDate businessDate) {
        RLock lock = redissonClient.getLock(lockKey(businessDate));
        try {
            boolean acquired = lock.tryLock(properties.getRecoveryLockWaitTime().toMillis(), TimeUnit.MILLISECONDS);
            if (!acquired) {
                log.warn("claim intake sign no recovery lock timeout date={}", businessDate);
                return new StaticHandle(LockAcquireStatus.TIMEOUT);
            }
            return new AcquiredHandle(lock, businessDate);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("claim intake sign no recovery lock interrupted date={}", businessDate);
            return new StaticHandle(LockAcquireStatus.TIMEOUT);
        } catch (RedisException exception) {
            log.warn("claim intake sign no recovery lock unavailable date={}", businessDate, exception);
            return new StaticHandle(LockAcquireStatus.UNAVAILABLE);
        }
    }

    private String lockKey(LocalDate businessDate) {
        return properties.getKeyPrefix() + RedisCacheConstants.RECONCILE_LOCK_SEGMENT
                + businessDate.format(RedisCacheConstants.DATE_KEY_FORMATTER);
    }

    /**
     * 未获取锁时关闭句柄不执行任何释放动作。
     */
    private static final class StaticHandle implements SignNoRecoveryLockHandle {

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
    private static final class AcquiredHandle implements SignNoRecoveryLockHandle {

        private final RLock lock;
        private final LocalDate businessDate;

        private AcquiredHandle(RLock lock, LocalDate businessDate) {
            this.lock = lock;
            this.businessDate = businessDate;
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
                    log.warn("claim intake sign no recovery lock ownership lost date={}", businessDate);
                }
            } catch (RedisException | IllegalMonitorStateException exception) {
                log.warn("claim intake sign no recovery lock release failed date={} type={}", businessDate,
                        exception.getClass().getSimpleName(), exception);
            }
        }
    }
}
