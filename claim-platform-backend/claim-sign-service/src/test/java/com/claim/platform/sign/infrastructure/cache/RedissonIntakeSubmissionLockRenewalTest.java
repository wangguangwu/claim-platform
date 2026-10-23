package com.claim.platform.sign.infrastructure.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.claim.platform.sign.application.port.IntakeSubmissionLockHandle;
import com.claim.platform.sign.application.port.LockAcquireStatus;
import com.claim.platform.sign.infrastructure.config.IntakeSubmissionLockProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIf;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 使用真实 Redis 验证 watchdog 在超过基础租期后仍保持互斥，并在释放后允许重新获取。
 * 本地 Redis 不可用时跳过，避免在无基础设施环境产生伪失败。
 *
 * @author wangguangwu
 */
@EnabledIf("isRedisAvailable")
class RedissonIntakeSubmissionLockRenewalTest {

    private static final long WATCHDOG_TIMEOUT_MILLIS = 1000L;
    private static final long HOLD_MILLIS = 2500L;

    private RedissonClient redissonClient;
    private RedissonIntakeSubmissionLock submissionLock;
    private String tenantCode;
    private String externalApplyNo;

    @BeforeEach
    void setUp() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://" + redisHost() + ":" + redisPort());
        config.setLockWatchdogTimeout(WATCHDOG_TIMEOUT_MILLIS);
        redissonClient = Redisson.create(config);

        IntakeSubmissionLockProperties properties = new IntakeSubmissionLockProperties();
        properties.setWaitTime(Duration.ofMillis(200));
        properties.setLeaseTime(Duration.ofMillis(WATCHDOG_TIMEOUT_MILLIS));
        submissionLock = new RedissonIntakeSubmissionLock(redissonClient, properties, new SimpleMeterRegistry());
        tenantCode = "it-renewal-" + UUID.randomUUID();
        externalApplyNo = "EXT-" + UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        if (redissonClient != null) {
            redissonClient.shutdown();
        }
    }

    @Test
    @Timeout(30)
    void shouldKeepMutexBeyondBaseLeaseAndAllowAcquireAfterRelease() throws InterruptedException {
        CountDownLatch acquired = new CountDownLatch(1);
        CountDownLatch released = new CountDownLatch(1);
        AtomicReference<Throwable> holderFailure = new AtomicReference<>();
        Thread holder = new Thread(() -> {
            try (IntakeSubmissionLockHandle handle = submissionLock.tryAcquire(tenantCode, externalApplyNo)) {
                assertThat(handle.status()).isEqualTo(LockAcquireStatus.ACQUIRED);
                acquired.countDown();
                Thread.sleep(HOLD_MILLIS);
            } catch (Throwable throwable) {
                holderFailure.set(throwable);
            } finally {
                released.countDown();
            }
        }, "intake-lock-holder");

        try {
            holder.start();
            assertThat(acquired.await(5, TimeUnit.SECONDS)).isTrue();

            // 超过基础租期后仍应由 watchdog 续期，其他线程无法获取
            Thread.sleep(WATCHDOG_TIMEOUT_MILLIS + 500L);
            try (IntakeSubmissionLockHandle handle = submissionLock.tryAcquire(tenantCode, externalApplyNo)) {
                assertThat(handle.status()).isEqualTo(LockAcquireStatus.TIMEOUT);
            }

            assertThat(released.await(10, TimeUnit.SECONDS)).isTrue();
            holder.join(5000L);
            assertThat(holderFailure.get()).isNull();

            try (IntakeSubmissionLockHandle handle = submissionLock.tryAcquire(tenantCode, externalApplyNo)) {
                assertThat(handle.status()).isEqualTo(LockAcquireStatus.ACQUIRED);
            }
        } finally {
            holder.interrupt();
        }
    }

    static boolean isRedisAvailable() {
        return isPortOpen(redisHost(), redisPort());
    }

    private static String redisHost() {
        return System.getenv().getOrDefault("REDIS_HOST", "localhost");
    }

    private static int redisPort() {
        return Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"));
    }

    private static boolean isPortOpen(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 300);
            return true;
        } catch (IOException exception) {
            return false;
        }
    }
}
