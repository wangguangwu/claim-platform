package com.claim.platform.intake.infrastructure.cache;

import com.claim.platform.intake.application.exception.SignNoUnavailableException;
import com.claim.platform.intake.application.exception.SignNoUnavailableReason;
import com.claim.platform.intake.application.port.SignNoIncrementResult;
import com.claim.platform.intake.application.port.SignNoSequenceCounter;
import com.claim.platform.intake.application.result.SignNoReconcileAllocationOutcome;
import com.claim.platform.intake.application.result.SignNoReconcileOutcome;
import com.claim.platform.intake.infrastructure.config.SignNoProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 使用 Redis 单个按日计数器实现签收号发号端口。
 * 只使用单 Key Lua 脚本，保证 Redis Cluster 下落在同一 slot；
 * 键只包含固定前缀和业务日期，不包含租户、人员信息或外部申请编号。
 *
 * @author wangguangwu
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisSignNoSequenceCounter implements SignNoSequenceCounter {

    private static final DateTimeFormatter DATE_KEY_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private static final long MISSING_MARKER = -1L;
    private static final long INVALID_MARKER = -2L;

    private final StringRedisTemplate redisTemplate;
    private final SignNoProperties properties;

    private final DefaultRedisScript<Long> allocateScript = script("allocate.lua", Long.class);
    private final DefaultRedisScript<List> reconcileAndAllocateScript =
            script("reconcile-and-allocate.lua", List.class);
    private final DefaultRedisScript<List> reconcileScript = script("reconcile-only.lua", List.class);

    @Override
    public SignNoIncrementResult tryIncrement(LocalDate businessDate) {
        Long result = execute(allocateScript, counterKey(businessDate), ttlMillis());
        if (result == null) {
            throw new SignNoUnavailableException(SignNoUnavailableReason.REDIS_UNAVAILABLE);
        }
        if (result == MISSING_MARKER) {
            return SignNoIncrementResult.Missing.INSTANCE;
        }
        if (result == INVALID_MARKER) {
            return SignNoIncrementResult.Invalid.INSTANCE;
        }
        if (result < 1) {
            throw new SignNoUnavailableException(SignNoUnavailableReason.REDIS_UNAVAILABLE);
        }
        return new SignNoIncrementResult.Allocated(result);
    }

    @Override
    @SuppressWarnings("unchecked")
    public SignNoReconcileAllocationOutcome reconcileAndIncrement(LocalDate businessDate, long dbHighWaterMark) {
        List<Object> result;
        try {
            result = (List<Object>) redisTemplate.execute(reconcileAndAllocateScript,
                    List.of(counterKey(businessDate)), String.valueOf(dbHighWaterMark), ttlMillis());
        } catch (DataAccessException exception) {
            log.warn("claim intake sign no counter reconcile and allocate failed date={}", businessDate, exception);
            throw new SignNoUnavailableException(SignNoUnavailableReason.REDIS_UNAVAILABLE, exception);
        }
        if (result == null || result.size() != 3) {
            throw new SignNoUnavailableException(SignNoUnavailableReason.REDIS_UNAVAILABLE);
        }
        long sequence = toLong(result.get(0));
        if (sequence < 1) {
            throw new SignNoUnavailableException(SignNoUnavailableReason.REDIS_UNAVAILABLE);
        }
        return new SignNoReconcileAllocationOutcome(sequence, toLong(result.get(1)), toLong(result.get(2)));
    }

    @Override
    @SuppressWarnings("unchecked")
    public SignNoReconcileOutcome reconcile(LocalDate businessDate, long dbHighWaterMark) {
        List<Object> result;
        try {
            result = (List<Object>) redisTemplate.execute(reconcileScript, List.of(counterKey(businessDate)),
                    String.valueOf(dbHighWaterMark), ttlMillis());
        } catch (DataAccessException exception) {
            log.warn("claim intake sign no counter reconcile failed date={}", businessDate, exception);
            throw new SignNoUnavailableException(SignNoUnavailableReason.REDIS_UNAVAILABLE, exception);
        }
        if (result == null || result.size() != 2) {
            throw new SignNoUnavailableException(SignNoUnavailableReason.REDIS_UNAVAILABLE);
        }
        return new SignNoReconcileOutcome(toLong(result.get(0)), toLong(result.get(1)));
    }

    private Long execute(DefaultRedisScript<Long> script, String key, String... arguments) {
        try {
            return redisTemplate.execute(script, List.of(key), (Object[]) arguments);
        } catch (DataAccessException exception) {
            log.warn("claim intake sign no redis command failed", exception);
            throw new SignNoUnavailableException(SignNoUnavailableReason.REDIS_UNAVAILABLE, exception);
        }
    }

    private String counterKey(LocalDate businessDate) {
        return properties.getKeyPrefix() + businessDate.format(DATE_KEY_FORMATTER);
    }

    private String ttlMillis() {
        return String.valueOf(properties.getCounterTtl().toMillis());
    }

    private long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            return Long.parseLong(text);
        }
        throw new SignNoUnavailableException(SignNoUnavailableReason.REDIS_UNAVAILABLE);
    }

    private static <T> DefaultRedisScript<T> script(String name, Class<T> resultType) {
        DefaultRedisScript<T> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("lua/sign-no/" + name));
        script.setResultType(resultType);
        return script;
    }
}
