package com.claim.platform.sign.infrastructure.cache;

import com.claim.platform.sign.application.exception.ConcurrentSignModificationException;
import com.claim.platform.sign.application.port.BusinessLock;
import java.time.Duration;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** Uses a short Redis lease only to serialize creation; MySQL remains the source of truth.
 *
 * @author wangguangwu
 */
@Component
public class RedisBusinessLock implements BusinessLock {
    private static final Duration LEASE = Duration.ofSeconds(10);
    private final StringRedisTemplate redisTemplate;

    public RedisBusinessLock(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** Executes only when the short creation lease is acquired, otherwise tells callers to retry. */
    @Override
    public <T> T execute(String key, LockedOperation<T> operation) {
        String lockKey = "claim:sign:create:lock:" + key;
        String owner = UUID.randomUUID().toString();
        if (!Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(lockKey, owner, LEASE))) {
            throw new ConcurrentSignModificationException(key, "locked", null);
        }
        try {
            return operation.run();
        } finally {
            String value = redisTemplate.opsForValue().get(lockKey);
            if (owner.equals(value)) {
                redisTemplate.delete(lockKey);
            }
        }
    }
}
