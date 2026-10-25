package com.claim.platform.sign.application.task;

import static org.assertj.core.api.Assertions.assertThat;

import com.claim.platform.sign.application.port.SignRecoveryPort;
import com.claim.platform.sign.domain.sign.model.Sign;
import com.claim.platform.sign.domain.sign.model.SignNo;
import com.claim.platform.sign.domain.sign.model.SignStatus;
import com.claim.platform.sign.domain.sign.repository.SignRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Verifies that compensation selects recovery actions without rewriting lifecycle state.
 *
 * @author wangguangwu
 */
class SignRecoveryServiceTest {
    @Test
    void shouldRepublishOnlyPendingAcceptAndResumeAcceptedSign() {
        AtomicInteger events = new AtomicInteger();
        AtomicInteger policyChecks = new AtomicInteger();
        SignRecoveryService service = new SignRecoveryService(new EmptyRepository(), event -> events.incrementAndGet(),
                new SignRecoveryPort() {
                    @Override public void resumePolicyCheck(Sign sign) { policyChecks.incrementAndGet(); }
                    @Override public boolean retryAutoUnderwriting(Sign sign) { return false; }
                    @Override public void resendRevoke(Sign sign) { }
                });
        Sign pending = new Sign("tenant-a", new SignNo("S00000000000001"), "external-1", null, 1);
        Sign accepted = new Sign("tenant-a", new SignNo("S00000000000002"), "external-2", null, 1);
        accepted.transitionTo(SignStatus.ACCEPT_SUCCESS);

        service.recover(pending);
        service.recover(accepted);

        assertThat(events).hasValue(1);
        assertThat(policyChecks).hasValue(1);
        assertThat(accepted.status()).isEqualTo(SignStatus.ACCEPT_SUCCESS);
    }

    private static final class EmptyRepository implements SignRepository {
        @Override public Optional<Sign> findByTenantAndExternalApplyNo(String tenantCode, String externalApplyNo) { return Optional.empty(); }
        @Override public Optional<Sign> findByTenantAndSignNo(String tenantCode, SignNo signNo) { return Optional.empty(); }
        @Override public Optional<SignNo> findHighestSignNo() { return Optional.empty(); }
        @Override public List<Sign> findStalledBefore(Instant cutoff, int limit) { return List.of(); }
        @Override public void save(Sign sign) { }
    }
}
