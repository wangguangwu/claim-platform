package com.claim.platform.sign.application.handler;

import com.claim.platform.sign.application.command.SignCommand;
import com.claim.platform.sign.application.result.SignResult;
import com.claim.platform.sign.domain.model.Material;
import com.claim.platform.sign.domain.model.SignStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 使用真实 MySQL 和 Redis 验证同一幂等键并发提交只创建一条签收事实。
 * 本地中间件不可用时跳过，避免在无基础设施环境产生伪失败。
 *
 * @author wangguangwu
 */
@SpringBootTest(properties = {
        "claim.sign.submission.lock.wait-time=10s",
        "claim.sign.submission.lock.lease-time=30s"
})
@EnabledIf("isLocalInfrastructureAvailable")
class SignSubmissionIntegrationTest {

    private static final int CONCURRENCY = 6;

    @Autowired
    private SignCommandHandler commandService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    static boolean isLocalInfrastructureAvailable() {
        return isPortOpen(System.getenv().getOrDefault("MYSQL_HOST", "localhost"), 3306)
                && isPortOpen(System.getenv().getOrDefault("REDIS_HOST", "localhost"), 6379);
    }

    @Test
    void shouldCreateSingleSignWhenConcurrentSubmissionsShareIdempotencyKey() throws Exception {
        String tenantCode = "it-" + UUID.randomUUID();
        String externalApplyNo = "EXT-" + UUID.randomUUID();
        SignCommand command = command(tenantCode, externalApplyNo);
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENCY);
        CountDownLatch ready = new CountDownLatch(CONCURRENCY);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<SignResult>> futures = new ArrayList<>();
            for (int index = 0; index < CONCURRENCY; index++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return commandService.submit(command);
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            Set<String> signNos = new HashSet<>();
            Set<SignStatus> statuses = new HashSet<>();
            for (Future<SignResult> future : futures) {
                SignResult result = future.get(60, TimeUnit.SECONDS);
                signNos.add(result.signNo());
                statuses.add(result.status());
            }

            assertThat(signNos).hasSize(1);
            assertThat(statuses).containsExactly(SignStatus.IMAGE_UPLOADED);
            assertThat(countRows("t_claim_intake", tenantCode)).isEqualTo(1);
            assertThat(countRows("t_claim_intake_material", tenantCode)).isEqualTo(1);
            assertThat(countRows("t_claim_intake_interaction_log", tenantCode)).isEqualTo(1);
            assertThat(countInteractionLogs(tenantCode, "SIGN_RECEIVE")).isEqualTo(1);
        } finally {
            executor.shutdownNow();
            cleanup(tenantCode);
        }
    }

    private SignCommand command(String tenantCode, String externalApplyNo) {
        Material material = new Material("BATCH-1", 1, "ID_CARD", "身份证.jpg", "/tmp/a", "/tmp/b");
        return new SignCommand(tenantCode, "trace-it", externalApplyNo, "集成测试", List.of(material), "{}");
    }

    private int countRows(String table, String tenantCode) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE tenant_code = ?",
                Integer.class, tenantCode);
        return count == null ? 0 : count;
    }

    private int countInteractionLogs(String tenantCode, String interactionType) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_claim_intake_interaction_log "
                + "WHERE tenant_code = ? AND interaction_type = ?", Integer.class, tenantCode, interactionType);
        return count == null ? 0 : count;
    }

    private void cleanup(String tenantCode) {
        jdbcTemplate.update("DELETE FROM t_claim_intake_material WHERE tenant_code = ?", tenantCode);
        jdbcTemplate.update("DELETE FROM t_claim_intake_interaction_log WHERE tenant_code = ?", tenantCode);
        jdbcTemplate.update("DELETE FROM t_claim_intake WHERE tenant_code = ?", tenantCode);
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
