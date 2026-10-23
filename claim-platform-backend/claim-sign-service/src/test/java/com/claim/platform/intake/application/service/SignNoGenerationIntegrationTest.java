package com.claim.platform.intake.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.claim.platform.intake.application.command.SubmitIntakeCommand;
import com.claim.platform.intake.application.exception.SignNoUnavailableException;
import com.claim.platform.intake.application.exception.SignNoUnavailableReason;
import com.claim.platform.intake.application.port.SignNoFallbackAllocator;
import com.claim.platform.intake.application.port.SignNoHighWaterMark;
import com.claim.platform.intake.application.result.ClaimIntakeResult;
import com.claim.platform.intake.application.result.ReconciliationStatus;
import com.claim.platform.intake.application.result.SignNoAllocation;
import com.claim.platform.intake.application.result.SignNoReconcileAllocationOutcome;
import com.claim.platform.intake.application.result.SignNoReconciliationResult;
import com.claim.platform.intake.application.result.SignNoRecoveryTrigger;
import com.claim.platform.intake.application.result.SignNoSource;
import com.claim.platform.intake.domain.model.SignNoFormat;
import com.claim.platform.intake.infrastructure.config.SignNoProperties;
import com.claim.platform.intake.infrastructure.scheduling.SignNoReconciliationScheduler;
import com.claim.platform.intake.interfaces.tenant.support.SupportTenantTestData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * 使用真实 MySQL 和 Redis 验证签收号发号、恢复、降级、并发和两个 HTTP 入口。
 * 覆盖 TEST-SNG-019 至 TEST-SNG-030、TEST-SNG-032 至 TEST-SNG-036；
 * 本地中间件不可用时跳过，发布证据必须单独记录实际执行结果。
 *
 * @author wangguangwu
 */
@SpringBootTest(properties = {
        "claim.internal.service-token=it-token",
        "claim.intake.number.key-prefix=sign:no:it-${random.uuid}:",
        "claim.intake.number.reconciliation-initial-delay=1h",
        "claim.intake.number.reconciliation-interval=1h",
        "claim.intake.submission.lock.wait-time=30s",
        "claim.intake.submission.lock.lease-time=60s"
})
@AutoConfigureMockMvc
@EnabledIf("isLocalInfrastructureAvailable")
class SignNoGenerationIntegrationTest {

    private static final String SERVICE_TOKEN = "it-token";
    private static final String TRACE_ID = "it-trace";
    private static final String TEST_TENANT_PREFIX = "it-sng-";
    private static final String TEST_TENANT_LIKE = TEST_TENANT_PREFIX + "%";
    private static final String REFERENCE_EXTERNAL_LIKE = "IT-SNG-%";
    private static final DateTimeFormatter DATE_KEY_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private static final int TENANT_CONCURRENCY = 100;

    @Autowired
    private ClaimIntakeCommandService commandService;
    @Autowired
    private SignNoAllocationService allocationService;
    @Autowired
    private SignNoReconciliationService reconciliationService;
    @Autowired
    private SignNoReconciliationScheduler scheduler;
    @Autowired
    private SignNoModeCoordinator modeCoordinator;
    @Autowired
    private SignNoHighWaterMark highWaterMark;
    @Autowired
    private SignNoProperties properties;
    @Autowired
    private Clock clock;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private MeterRegistry meterRegistry;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @SpyBean
    private SignNoFallbackAllocator fallbackAllocator;

    static boolean isLocalInfrastructureAvailable() {
        return isPortOpen(System.getenv().getOrDefault("MYSQL_HOST", "localhost"), 3306)
                && isPortOpen(System.getenv().getOrDefault("REDIS_HOST", "localhost"), 6379);
    }

    @BeforeEach
    void prepareEmptyBusinessDate() throws Exception {
        properties.setFallbackEnabled(true);
        cleanupDatabase();
        cleanupRedis();
        SignNoReconciliationResult calibration = scheduler.requestAsync(today())
                .get(15, TimeUnit.SECONDS);
        assertThat(calibration.status()).isEqualTo(ReconciliationStatus.SUCCESS);
        assertThat(modeCoordinator.isRedisReady()).isTrue();
    }

    @AfterEach
    void cleanUp() {
        properties.setFallbackEnabled(true);
        cleanupDatabase();
        cleanupRedis();
    }

    @Test
    void shouldAllocateUniqueSignNumbersForHundredConcurrentSubmissions() throws Exception {
        String tenantCode = newTenant();
        List<Attempt<ClaimIntakeResult>> attempts = runConcurrently(TENANT_CONCURRENCY,
                () -> commandService.submit(command(tenantCode, newExternalApplyNo())));

        assertThat(attempts).allMatch(Attempt::succeeded);
        Set<String> signNos = new HashSet<>();
        for (Attempt<ClaimIntakeResult> attempt : attempts) {
            signNos.add(attempt.value().signNo());
        }
        assertThat(signNos).hasSize(TENANT_CONCURRENCY);
        assertThat(signNos).allMatch(SignNoFormat::isCurrentFormat);
        assertThat(signNos).allMatch(signNo -> signNo.startsWith(datePrefix(today())));
        assertThat(countRows(tenantCode)).isEqualTo(TENANT_CONCURRENCY);
    }

    @Test
    void shouldRecoverFromDeletedAndInvalidCounterUsingDatabaseHighWater() throws Exception {
        String tenantCode = newTenant();
        long base = highWaterMark.currentMaxSequence(today());
        insertIntake(tenantCode, SignNoFormat.format(today(), base + 7), newExternalApplyNo(), false);

        deleteCounter(today());
        SignNoAllocation missingRecovery = allocationService.next();
        assertThat(missingRecovery.source()).isEqualTo(SignNoSource.REDIS);
        assertThat(sequence(missingRecovery.signNo())).isEqualTo(base + 8);
        assertThat(counterValue(today())).isEqualTo(base + 8);

        insertIntake(tenantCode, missingRecovery.signNo(), newExternalApplyNo(), false);
        deleteCounter(today());
        setCounter(today(), "not-a-sequence");
        SignNoAllocation invalidRecovery = allocationService.next();
        assertThat(invalidRecovery.source()).isEqualTo(SignNoSource.REDIS);
        assertThat(sequence(invalidRecovery.signNo())).isEqualTo(base + 9);
        assertThat(counterValue(today())).isEqualTo(base + 9);

        LocalDate futureDate = today().plusDays(30);
        deleteCounter(futureDate);
        setCounter(futureDate, "-5");
        SignNoReconcileAllocationOutcome zeroHighWater =
                reconciliationService.reconcileAndAllocate(futureDate, SignNoRecoveryTrigger.INVALID_COUNTER);
        assertThat(zeroHighWater.sequence()).isEqualTo(1);
        assertThat(zeroHighWater.counterBefore()).isZero();
        assertThat(zeroHighWater.counterAfter()).isZero();
        assertThat(counterValue(futureDate)).isEqualTo(1);
    }

    @Test
    void shouldRecoverFromCounterRollbackAndSignNoCollision() throws Exception {
        String tenantCode = newTenant();
        long base = highWaterMark.currentMaxSequence(today());
        insertIntake(tenantCode, SignNoFormat.format(today(), base + 1), newExternalApplyNo(), false);
        setCounter(today(), String.valueOf(base));
        double conflictBefore = counterCount("claim.intake.number.conflict", "type", "sign_no");
        double recoveryBefore = counterCount("claim.intake.number.recovery",
                "trigger", SignNoRecoveryTrigger.UNIQUE_CONFLICT.code(), "result", "success");

        ClaimIntakeResult result = commandService.submit(command(tenantCode, newExternalApplyNo()));

        assertThat(sequence(result.signNo())).isEqualTo(base + 2);
        assertThat(counterValue(today())).isEqualTo(base + 2);
        assertThat(countRows(tenantCode, result.signNo())).isEqualTo(1);
        assertThat(counterCount("claim.intake.number.conflict", "type", "sign_no") - conflictBefore)
                .isEqualTo(1.0);
        assertThat(counterCount("claim.intake.number.recovery",
                "trigger", SignNoRecoveryTrigger.UNIQUE_CONFLICT.code(), "result", "success") - recoveryBefore)
                .isEqualTo(1.0);
    }

    @Test
    void shouldCountSoftDeletedRowsInHighWaterMark() throws Exception {
        String tenantCode = newTenant();
        long base = highWaterMark.currentMaxSequence(today());
        insertIntake(tenantCode, SignNoFormat.format(today(), base + 9), newExternalApplyNo(), true);

        deleteCounter(today());
        SignNoAllocation allocation = allocationService.next();

        assertThat(sequence(allocation.signNo())).isEqualTo(base + 10);
    }

    @Test
    void shouldIgnoreHistoricalSignNumbersAndKeepThemQueryable() throws Exception {
        String tenantCode = newTenant();
        String historicalSignNo = "SIGN0123456789abcdef0123";
        String externalApplyNo = newExternalApplyNo();
        insertIntake(tenantCode, historicalSignNo, externalApplyNo, false);
        long base = highWaterMark.currentMaxSequence(today());

        deleteCounter(today());
        SignNoAllocation allocation = allocationService.next();

        assertThat(sequence(allocation.signNo())).isEqualTo(base + 1);
        assertThat(SignNoFormat.isCurrentFormat(historicalSignNo)).isFalse();
        mockMvc.perform(gateway(get("/api/v1/claim-intakes/{signNo}", historicalSignNo), tenantCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.signNo").value(historicalSignNo))
                .andExpect(jsonPath("$.data.externalApplyNo").value(externalApplyNo));
    }

    @Test
    void shouldDegradeToFallbackWhenRedisUnavailable() throws Exception {
        modeCoordinator.markRedisUnavailable();
        String tenantCode = newTenant();
        long base = highWaterMark.currentMaxSequence(today());
        double fallbackBefore = counterCount("claim.intake.number.fallback", "result", "success");

        ClaimIntakeResult result = commandService.submit(command(tenantCode, newExternalApplyNo()));

        assertThat(SignNoFormat.isCurrentFormat(result.signNo())).isTrue();
        assertThat(sequence(result.signNo())).isEqualTo(base + 1);
        assertThat(countRows(tenantCode, result.signNo())).isEqualTo(1);
        assertThat(counterValue(today())).isLessThan(sequence(result.signNo()));
        assertThat(counterCount("claim.intake.number.fallback", "result", "success") - fallbackBefore)
                .isEqualTo(1.0);
        assertThat(modeCoordinator.isRedisReady()).isFalse();
    }

    @Test
    void shouldHandleConcurrentFallbackWithBoundedFailures() throws Exception {
        modeCoordinator.markRedisUnavailable();
        String tenantCode = newTenant();
        long base = highWaterMark.currentMaxSequence(today());

        List<Attempt<ClaimIntakeResult>> attempts = runConcurrently(20,
                () -> commandService.submit(command(tenantCode, newExternalApplyNo())));

        List<String> signNos = new ArrayList<>();
        List<Throwable> failures = new ArrayList<>();
        for (Attempt<ClaimIntakeResult> attempt : attempts) {
            if (attempt.succeeded()) {
                signNos.add(attempt.value().signNo());
            } else {
                failures.add(attempt.error());
            }
        }
        assertThat(signNos).isNotEmpty();
        assertThat(signNos).doesNotHaveDuplicates();
        assertThat(signNos).allMatch(signNo -> sequence(signNo) > base);
        assertThat(failures).allMatch(SignNoUnavailableException.class::isInstance);
        assertThat(signNos.size() + failures.size()).isEqualTo(20);
        assertThat(countRows(tenantCode)).isEqualTo(signNos.size());
    }

    @Test
    void shouldCalibrateRedisAndContinueAboveDatabaseMaxAfterRecovery() throws Exception {
        String tenantCode = newTenant();
        long base = highWaterMark.currentMaxSequence(today());
        insertIntake(tenantCode, SignNoFormat.format(today(), base + 4), newExternalApplyNo(), false);
        setCounter(today(), String.valueOf(base + 1));

        SignNoReconciliationResult result = scheduler.requestAsync(today()).get(15, TimeUnit.SECONDS);

        assertThat(result.status()).isEqualTo(ReconciliationStatus.SUCCESS);
        assertThat(result.dbHighWaterMark()).isEqualTo(base + 4);
        assertThat(result.counterAfter()).isEqualTo(base + 4);
        assertThat(result.sequenceGap()).isEqualTo(3);
        assertThat(modeCoordinator.isRedisReady()).isTrue();
        assertThat(sequence(allocationService.next().signNo())).isEqualTo(base + 5);
    }

    @Test
    void shouldReconcileMissingCounterWithoutConsumingSequence() throws Exception {
        String tenantCode = newTenant();
        long base = highWaterMark.currentMaxSequence(today());
        insertIntake(tenantCode, SignNoFormat.format(today(), base + 4), newExternalApplyNo(), false);
        deleteCounter(today());

        SignNoReconciliationResult result = scheduler.requestAsync(today()).get(15, TimeUnit.SECONDS);

        assertThat(result.status()).isEqualTo(ReconciliationStatus.SUCCESS);
        assertThat(result.counterBefore()).isZero();
        assertThat(result.counterAfter()).isEqualTo(base + 4);
        assertThat(counterValue(today())).isEqualTo(base + 4);
        assertThat(sequence(allocationService.next().signNo())).isEqualTo(base + 5);
    }

    @Test
    void shouldReturnUnavailableAndWriteNothingWhenFallbackDisabled() {
        modeCoordinator.markRedisUnavailable();
        properties.setFallbackEnabled(false);
        String tenantCode = newTenant();
        try {
            assertThatThrownBy(() -> commandService.submit(command(tenantCode, newExternalApplyNo())))
                    .isInstanceOf(SignNoUnavailableException.class)
                    .extracting(exception -> ((SignNoUnavailableException) exception).reason())
                    .isEqualTo(SignNoUnavailableReason.FALLBACK_DISABLED);
            assertThat(countRows(tenantCode)).isZero();
        } finally {
            properties.setFallbackEnabled(true);
        }
    }

    @Test
    void shouldReturnUnavailableAndWriteNothingWhenBothPathsUnavailable() {
        modeCoordinator.markRedisUnavailable();
        String tenantCode = newTenant();
        doThrow(new SignNoUnavailableException(SignNoUnavailableReason.HIGH_WATER_MARK_UNAVAILABLE))
                .when(fallbackAllocator).nextSequence(any(LocalDate.class));
        try {
            assertThatThrownBy(() -> commandService.submit(command(tenantCode, newExternalApplyNo())))
                    .isInstanceOf(SignNoUnavailableException.class)
                    .extracting(exception -> ((SignNoUnavailableException) exception).reason())
                    .isEqualTo(SignNoUnavailableReason.HIGH_WATER_MARK_UNAVAILABLE);
            assertThat(countRows(tenantCode)).isZero();
        } finally {
            reset(fallbackAllocator);
        }
    }

    @Test
    void shouldRejectCrossTenantDuplicateSignNoByGlobalUniqueIndex() {
        long base = highWaterMark.currentMaxSequence(today());
        String signNo = SignNoFormat.format(today(), base + 1);
        String firstTenant = newTenant();
        String secondTenant = newTenant();
        insertIntake(firstTenant, signNo, newExternalApplyNo(), false);

        assertThatThrownBy(() -> insertIntake(secondTenant, signNo, newExternalApplyNo(), false))
                .isInstanceOf(DuplicateKeyException.class);
        assertThat(countRows(secondTenant)).isZero();
    }

    @Test
    void shouldServeNewFormatFromBothHttpEntryPoints() throws Exception {
        String standardTenant = newTenant();
        String standardExternalApplyNo = "IT-SNG-STD-" + UUID.randomUUID();
        MvcResult standard = mockMvc.perform(gateway(post("/api/v1/claim-intakes"), standardTenant)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"externalApplyNo\":\"" + standardExternalApplyNo + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.traceId").value(TRACE_ID))
                .andExpect(jsonPath("$.data.signNo", matchesPattern("^S\\d{16}$")))
                .andReturn();
        String standardSignNo = responseData(standard, "signNo");
        assertThat(standardSignNo).startsWith(datePrefix(today()));

        String referenceExternalApplyNo = "IT-SNG-REF-" + UUID.randomUUID();
        String payload = SupportTenantTestData.VALID_PAYLOAD.replace("EXT-001", referenceExternalApplyNo);
        MvcResult reference = mockMvc.perform(gateway(post("/api/v1/tenant-intakes"), "reference")
                        .header("X-Claim-Protocol-Version", "v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.signNo", matchesPattern("^S\\d{16}$")))
                .andReturn();
        String referenceSignNo = responseData(reference, "signNo");

        assertThat(referenceSignNo).startsWith(datePrefix(today()));
        assertThat(referenceSignNo).isNotEqualTo(standardSignNo);
    }

    @Test
    void shouldExpireCounterWithConfiguredTtl() {
        allocationService.next();

        Long ttlMillis = redisTemplate.getExpire(counterKey(today()), TimeUnit.MILLISECONDS);

        assertThat(ttlMillis).isNotNull().isPositive();
        assertThat(ttlMillis).isLessThanOrEqualTo(properties.getCounterTtl().toMillis());
        assertThat(ttlMillis).isGreaterThan(Duration.ofDays(2).toMillis());
    }

    @Test
    void shouldIsolateCountersByBusinessDateAndZone() {
        LocalDate firstDate = today().plusDays(1);
        LocalDate secondDate = today().plusDays(2);
        deleteCounter(firstDate);
        deleteCounter(secondDate);

        SignNoReconcileAllocationOutcome first = reconciliationService.reconcileAndAllocate(firstDate,
                SignNoRecoveryTrigger.MISSING_COUNTER);
        SignNoReconcileAllocationOutcome second = reconciliationService.reconcileAndAllocate(secondDate,
                SignNoRecoveryTrigger.MISSING_COUNTER);

        assertThat(first.sequence()).isEqualTo(1);
        assertThat(second.sequence()).isEqualTo(1);
        assertThat(counterValue(firstDate)).isEqualTo(1);
        assertThat(counterValue(secondDate)).isEqualTo(1);
        assertThat(counterKey(firstDate)).isNotEqualTo(counterKey(secondDate));
        assertThat(counterKey(firstDate)).endsWith(firstDate.format(DATE_KEY_FORMATTER));
        assertThat(clock.getZone()).isEqualTo(ZoneId.of("Asia/Shanghai"));
        assertThat(clock.getZone()).isEqualTo(properties.getZone());
        assertThat(SignNoFormat.format(LocalDate.now(clock), 1)).startsWith(datePrefix(LocalDate.now(clock)));
    }

    @Test
    void shouldRecordSequenceGapMetricDuringCalibration() throws Exception {
        long base = highWaterMark.currentMaxSequence(today());
        insertIntake(newTenant(), SignNoFormat.format(today(), base + 5), newExternalApplyNo(), false);
        setCounter(today(), String.valueOf(base + 2));

        SignNoReconciliationResult result = scheduler.requestAsync(today()).get(15, TimeUnit.SECONDS);

        assertThat(result.sequenceGap()).isEqualTo(3);
        Gauge gauge = meterRegistry.get("claim.intake.number.sequence_gap").tag("scope", "current").gauge();
        assertThat(gauge.value()).isEqualTo(3.0);
    }

    @Test
    void shouldKeepSingleSignNoForConcurrentDuplicateSubmissions() throws Exception {
        String tenantCode = newTenant();
        String externalApplyNo = newExternalApplyNo();

        List<Attempt<ClaimIntakeResult>> attempts = runConcurrently(8,
                () -> commandService.submit(command(tenantCode, externalApplyNo)));

        assertThat(attempts).allMatch(Attempt::succeeded);
        Set<String> signNos = new HashSet<>();
        for (Attempt<ClaimIntakeResult> attempt : attempts) {
            signNos.add(attempt.value().signNo());
        }
        assertThat(signNos).hasSize(1);
        assertThat(countRows(tenantCode)).isEqualTo(1);
    }

    private SubmitIntakeCommand command(String tenantCode, String externalApplyNo) {
        return new SubmitIntakeCommand(tenantCode, TRACE_ID, externalApplyNo, "集成测试", null, null, null,
                null, List.of(), "{}");
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private String newTenant() {
        return TEST_TENANT_PREFIX + UUID.randomUUID();
    }

    private String newExternalApplyNo() {
        return "EXT-" + UUID.randomUUID();
    }

    private String datePrefix(LocalDate date) {
        return SignNoFormat.PREFIX + date.format(DATE_KEY_FORMATTER);
    }

    private long sequence(String signNo) {
        return SignNoFormat.parseSequence(signNo);
    }

    private String counterKey(LocalDate date) {
        return properties.getKeyPrefix() + date.format(DATE_KEY_FORMATTER);
    }

    private void deleteCounter(LocalDate date) {
        redisTemplate.delete(counterKey(date));
    }

    private void setCounter(LocalDate date, String value) {
        redisTemplate.opsForValue().set(counterKey(date), value);
    }

    private long counterValue(LocalDate date) {
        String value = redisTemplate.opsForValue().get(counterKey(date));
        return value == null ? -1 : Long.parseLong(value);
    }

    private void insertIntake(String tenantCode, String signNo, String externalApplyNo, boolean deleted) {
        jdbcTemplate.update("""
                INSERT INTO t_claim_intake (tenant_code, sign_no, external_apply_no, applicant_name,
                                            intake_status, is_deleted)
                VALUES (?, ?, ?, ?, 'IMAGE_NOT_UPLOAD', ?)
                """, tenantCode, signNo, externalApplyNo, "集成测试", deleted);
    }

    private int countRows(String tenantCode) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_claim_intake WHERE tenant_code = ?", Integer.class, tenantCode);
        return count == null ? 0 : count;
    }

    private int countRows(String tenantCode, String signNo) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_claim_intake WHERE tenant_code = ? AND sign_no = ?",
                Integer.class, tenantCode, signNo);
        return count == null ? 0 : count;
    }

    private double counterCount(String name, String... tags) {
        return meterRegistry.counter(name, tags).count();
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder gateway(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder, String tenantCode) {
        return builder.header("X-Claim-Service-Token", SERVICE_TOKEN)
                .header("X-Claim-Tenant", tenantCode)
                .header("X-Trace-Id", TRACE_ID);
    }

    private String responseData(MvcResult result, String field) throws IOException {
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        return body.path("data").path(field).asText();
    }

    private void cleanupDatabase() {
        jdbcTemplate.update("DELETE FROM t_claim_intake_material WHERE tenant_code LIKE ?", TEST_TENANT_LIKE);
        jdbcTemplate.update("DELETE FROM t_claim_intake_interaction_log WHERE tenant_code LIKE ?",
                TEST_TENANT_LIKE);
        jdbcTemplate.update("DELETE FROM t_claim_intake WHERE tenant_code LIKE ?", TEST_TENANT_LIKE);
        jdbcTemplate.update("""
                DELETE FROM t_claim_intake_material
                WHERE sign_no IN (SELECT sign_no FROM t_claim_intake
                                  WHERE tenant_code = 'reference' AND external_apply_no LIKE ?)
                """, REFERENCE_EXTERNAL_LIKE);
        jdbcTemplate.update("""
                DELETE FROM t_claim_intake_interaction_log
                WHERE sign_no IN (SELECT sign_no FROM t_claim_intake
                                  WHERE tenant_code = 'reference' AND external_apply_no LIKE ?)
                """, REFERENCE_EXTERNAL_LIKE);
        jdbcTemplate.update("DELETE FROM t_claim_intake WHERE tenant_code = 'reference' "
                + "AND external_apply_no LIKE ?", REFERENCE_EXTERNAL_LIKE);
    }

    private void cleanupRedis() {
        Set<String> keys = redisTemplate.keys(properties.getKeyPrefix() + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private <T> List<Attempt<T>> runConcurrently(int count, Supplier<T> task) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(count);
        CountDownLatch ready = new CountDownLatch(count);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Attempt<T>>> futures = new ArrayList<>();
        try {
            for (int index = 0; index < count; index++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        return new Attempt<>(task.get(), null);
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        return new Attempt<>(null, exception);
                    } catch (Throwable throwable) {
                        return new Attempt<>(null, throwable);
                    }
                }));
            }
            assertThat(ready.await(30, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<Attempt<T>> attempts = new ArrayList<>();
            for (Future<Attempt<T>> future : futures) {
                attempts.add(future.get(180, TimeUnit.SECONDS));
            }
            return attempts;
        } catch (ExecutionException | TimeoutException exception) {
            throw new AssertionError(exception);
        } finally {
            executor.shutdownNow();
        }
    }

    private static boolean isPortOpen(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 300);
            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    private record Attempt<T>(T value, Throwable error) {

        private boolean succeeded() {
            return error == null;
        }
    }
}
