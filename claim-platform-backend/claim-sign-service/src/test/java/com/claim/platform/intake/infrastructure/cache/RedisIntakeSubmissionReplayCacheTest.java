package com.claim.platform.intake.infrastructure.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.claim.platform.intake.infrastructure.config.IntakeSubmissionReplayCacheProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

/**
 * 验证 Redis 幂等定位缓存的摘要键、TTL、指标和不可用降级语义。
 *
 * @author wangguangwu
 */
@ExtendWith(MockitoExtension.class)
class RedisIntakeSubmissionReplayCacheTest {

    private static final String CACHE_METRIC = "claim.intake.submission.replay-cache";

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final IntakeSubmissionReplayCacheProperties properties = new IntakeSubmissionReplayCacheProperties();
    private RedisIntakeSubmissionReplayCache replayCache;

    @BeforeEach
    void setUp() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        replayCache = new RedisIntakeSubmissionReplayCache(redisTemplate, properties, meterRegistry);
    }

    @Test
    void shouldReadTenantScopedHashedCacheKey() {
        given(valueOperations.get(anyString())).willReturn("SIGN001");

        assertThat(replayCache.findSignNo("tenant-a", "EXT001")).contains("SIGN001");

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).get(keyCaptor.capture());
        String key = keyCaptor.getValue();
        assertThat(key).isEqualTo("claim:intake:replay:" + sha256("tenant-a\nEXT001"));
        assertThat(key).doesNotContain("tenant-a", "EXT001");
        assertThat(cacheCount("read", "hit")).isEqualTo(1.0);
    }

    @Test
    void shouldWriteWithConfiguredTtl() {
        properties.setTtl(Duration.ofHours(2));

        replayCache.put("tenant-a", "EXT001", "SIGN001");

        verify(valueOperations).set("claim:intake:replay:" + sha256("tenant-a\nEXT001"), "SIGN001",
                Duration.ofHours(2));
        assertThat(cacheCount("write", "success")).isEqualTo(1.0);
    }

    @Test
    void shouldTreatReadFailureAsCacheMiss() {
        given(valueOperations.get(anyString())).willThrow(new RedisConnectionFailureException("redis down"));

        assertThat(replayCache.findSignNo("tenant-a", "EXT001")).isEmpty();
        assertThat(cacheCount("read", "unavailable")).isEqualTo(1.0);
    }

    @Test
    void shouldSwallowWriteAndEvictionFailures() {
        doThrow(new RedisConnectionFailureException("redis down"))
                .when(valueOperations).set(anyString(), anyString(), any(Duration.class));
        doThrow(new RedisConnectionFailureException("redis down")).when(redisTemplate).delete(anyString());

        assertThatCode(() -> replayCache.put("tenant-a", "EXT001", "SIGN001")).doesNotThrowAnyException();
        assertThatCode(() -> replayCache.evict("tenant-a", "EXT001")).doesNotThrowAnyException();
        assertThat(cacheCount("write", "unavailable")).isEqualTo(1.0);
        assertThat(cacheCount("evict", "unavailable")).isEqualTo(1.0);
    }

    private double cacheCount(String operation, String result) {
        Counter counter = meterRegistry.find(CACHE_METRIC).tags("operation", operation, "result", result).counter();
        return counter == null ? 0.0 : counter.count();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
