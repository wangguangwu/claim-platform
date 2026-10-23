package com.claim.platform.sign.infrastructure.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.claim.platform.sign.application.port.SignSubmissionLockHandle;
import com.claim.platform.sign.application.port.LockAcquireStatus;
import com.claim.platform.sign.infrastructure.config.SignSubmissionLockProperties;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisConnectionException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 验证 Redisson 锁的租户隔离键、watchdog 获取方式、降级、中断和所有权安全释放。
 *
 * @author wangguangwu
 */
@ExtendWith(MockitoExtension.class)
class RedissonSignSubmissionLockTest {

    private static final String LOCK_METRIC = "claim.intake.submission.lock";

    @Mock
    private RedissonClient redissonClient;
    @Mock
    private RLock lock;

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private RedissonSignSubmissionLock submissionLock;

    @BeforeEach
    void setUp() {
        submissionLock = new RedissonSignSubmissionLock(redissonClient,
                new SignSubmissionLockProperties(), meterRegistry);
    }

    @Test
    void shouldUseTenantScopedHashedKey() throws InterruptedException {
        given(redissonClient.getLock(anyString())).willReturn(lock);
        given(lock.tryLock(3000L, TimeUnit.MILLISECONDS)).willReturn(true);
        given(lock.isHeldByCurrentThread()).willReturn(true);

        submissionLock.tryAcquire("tenant-a", "EXT001").close();
        submissionLock.tryAcquire("tenant-b", "EXT001").close();

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(redissonClient, times(2)).getLock(keyCaptor.capture());
        List<String> keys = keyCaptor.getAllValues();
        assertThat(keys).doesNotHaveDuplicates();
        assertThat(keys.get(0)).isEqualTo("claim:intake:submit:" + sha256("tenant-a\nEXT001"));
        assertThat(keys.get(1)).isEqualTo("claim:intake:submit:" + sha256("tenant-b\nEXT001"));
        assertThat(keys).allSatisfy(key -> assertThat(key).doesNotContain("tenant-a", "tenant-b", "EXT001"));
    }

    @Test
    void shouldAcquireWithWatchdogLease() throws InterruptedException {
        given(redissonClient.getLock(anyString())).willReturn(lock);
        given(lock.tryLock(3000L, TimeUnit.MILLISECONDS)).willReturn(true);
        given(lock.isHeldByCurrentThread()).willReturn(true);

        SignSubmissionLockHandle handle = submissionLock.tryAcquire("tenant-a", "EXT001");

        assertThat(handle.status()).isEqualTo(LockAcquireStatus.ACQUIRED);
        handle.close();
        verify(lock).tryLock(3000L, TimeUnit.MILLISECONDS);
    }

    @Test
    void shouldReturnTimeoutWhenLockNotAcquired() throws InterruptedException {
        given(redissonClient.getLock(anyString())).willReturn(lock);
        given(lock.tryLock(3000L, TimeUnit.MILLISECONDS)).willReturn(false);

        SignSubmissionLockHandle handle = submissionLock.tryAcquire("tenant-a", "EXT001");
        handle.close();

        assertThat(handle.status()).isEqualTo(LockAcquireStatus.TIMEOUT);
        verify(lock, never()).unlock();
        assertThat(lockTimerCount("timeout")).isEqualTo(1);
    }

    @Test
    void shouldReturnUnavailableWhenRedisFails() throws InterruptedException {
        given(redissonClient.getLock(anyString())).willReturn(lock);
        given(lock.tryLock(3000L, TimeUnit.MILLISECONDS))
                .willThrow(new RedisConnectionException("redis down"));

        SignSubmissionLockHandle handle = submissionLock.tryAcquire("tenant-a", "EXT001");
        handle.close();

        assertThat(handle.status()).isEqualTo(LockAcquireStatus.UNAVAILABLE);
        verify(lock, never()).unlock();
        assertThat(lockTimerCount("unavailable")).isEqualTo(1);
    }

    @Test
    void shouldRestoreInterruptFlagWhenInterrupted() throws InterruptedException {
        given(redissonClient.getLock(anyString())).willReturn(lock);
        given(lock.tryLock(3000L, TimeUnit.MILLISECONDS))
                .willThrow(new InterruptedException("interrupted"));

        try {
            SignSubmissionLockHandle handle = submissionLock.tryAcquire("tenant-a", "EXT001");

            assertThat(handle.status()).isEqualTo(LockAcquireStatus.TIMEOUT);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
        assertThat(lockTimerCount("timeout")).isEqualTo(1);
    }

    @Test
    void shouldReleaseLockOnlyWhenHeldByCurrentThread() throws InterruptedException {
        given(redissonClient.getLock(anyString())).willReturn(lock);
        given(lock.tryLock(3000L, TimeUnit.MILLISECONDS)).willReturn(true);
        given(lock.isHeldByCurrentThread()).willReturn(true);

        submissionLock.tryAcquire("tenant-a", "EXT001").close();

        verify(lock).unlock();
    }

    @Test
    void shouldNotForceUnlockWhenOwnershipLost() throws InterruptedException {
        given(redissonClient.getLock(anyString())).willReturn(lock);
        given(lock.tryLock(3000L, TimeUnit.MILLISECONDS)).willReturn(true);
        given(lock.isHeldByCurrentThread()).willReturn(false);

        submissionLock.tryAcquire("tenant-a", "EXT001").close();

        verify(lock, never()).unlock();
    }

    @Test
    void shouldSwallowReleaseFailure() throws InterruptedException {
        given(redissonClient.getLock(anyString())).willReturn(lock);
        given(lock.tryLock(3000L, TimeUnit.MILLISECONDS)).willReturn(true);
        given(lock.isHeldByCurrentThread()).willReturn(true);
        willThrow(new RedisConnectionException("redis down")).given(lock).unlock();
        SignSubmissionLockHandle handle = submissionLock.tryAcquire("tenant-a", "EXT001");

        assertThatCode(handle::close).doesNotThrowAnyException();
    }

    private long lockTimerCount(String result) {
        Timer timer = meterRegistry.find(LOCK_METRIC).tag("result", result).timer();
        return timer == null ? 0 : timer.count();
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
