package com.claim.platform.sign.infrastructure.cache;

import com.claim.platform.sign.application.port.SignNumberGenerator;
import com.claim.platform.sign.domain.sign.model.SignNo;
import com.claim.platform.sign.domain.sign.repository.SignRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** Allocates monotonic local Sign numbers; database uniqueness remains the ultimate collision guard.
 *
 * @author wangguangwu
 */
@Component
public class RedisSignNumberGenerator implements SignNumberGenerator {
    private final StringRedisTemplate redisTemplate;
    private final SignRepository signRepository;
    public RedisSignNumberGenerator(StringRedisTemplate redisTemplate, SignRepository signRepository) {
        this.redisTemplate = redisTemplate;
        this.signRepository = signRepository;
    }
    /** Restores a missing Redis counter from the global Sign fact store before incrementing it. */
    @Override public SignNo next() {
        String key = "claim:sign:number";
        if (redisTemplate.opsForValue().get(key) == null) {
            long restored = signRepository.findHighestSignNo().map(this::sequenceOf).orElse(0L);
            redisTemplate.opsForValue().setIfAbsent(key, Long.toString(restored));
        }
        Long sequence = redisTemplate.opsForValue().increment(key);
        if (sequence == null) { throw new IllegalStateException("sign number unavailable"); }
        return new SignNo("S%014d".formatted(sequence));
    }

    private long sequenceOf(SignNo signNo) {
        try {
            return Long.parseLong(signNo.value().substring(1));
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Unsupported persisted signNo=" + signNo.value(), exception);
        }
    }
}
