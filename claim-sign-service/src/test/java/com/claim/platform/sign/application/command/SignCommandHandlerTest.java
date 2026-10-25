package com.claim.platform.sign.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import com.claim.platform.common.tenant.TenantContext;
import com.claim.platform.sign.application.port.BusinessLock;
import com.claim.platform.sign.application.port.EventPublisher;
import com.claim.platform.sign.application.port.SignAcceptedEvent;
import com.claim.platform.sign.application.port.SignNumberGenerator;
import com.claim.platform.sign.application.result.SignResult;
import com.claim.platform.sign.domain.interactionlog.repository.InteractionLogRepository;
import com.claim.platform.sign.domain.material.model.Material;
import com.claim.platform.sign.domain.material.repository.MaterialRepository;
import com.claim.platform.sign.domain.sign.model.Sign;
import com.claim.platform.sign.domain.sign.model.SignNo;
import com.claim.platform.sign.domain.sign.repository.SignRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Verifies creation idempotency and post-commit event intent without HTTP or external protocol fixtures.
 *
 * @author wangguangwu
 */
class SignCommandHandlerTest {
    @Test
    void shouldCreateFactAndPublishIntentOnlyForWinningRequest() {
        InMemorySignRepository signs = new InMemorySignRepository();
        List<SignAcceptedEvent> events = new ArrayList<>();
        SignCommandHandler handler = handler(signs, events);
        CreateSignCommand command = new CreateSignCommand(new TenantContext("tenant-a"), "apply-1", "Alice", 1,
                List.of(new InitialMaterial("batch-1", "IMAGE", "a.png", "oss://a", null)));

        SignResult first = handler.create(command);
        SignResult replay = handler.create(command);

        assertThat(first.replayed()).isFalse();
        assertThat(replay).isEqualTo(new SignResult(first.signNo(), "PENDING_ACCEPT", true));
        assertThat(events).hasSize(1);
    }

    @Test
    void shouldIsolateSameExternalApplyNoAcrossTenants() {
        InMemorySignRepository signs = new InMemorySignRepository();
        List<SignAcceptedEvent> events = new ArrayList<>();
        SignCommandHandler handler = handler(signs, events);

        SignResult first = handler.create(new CreateSignCommand(new TenantContext("tenant-a"), "apply-1", null, 1, List.of()));
        SignResult second = handler.create(new CreateSignCommand(new TenantContext("tenant-b"), "apply-1", null, 1, List.of()));

        assertThat(first.signNo()).isNotEqualTo(second.signNo());
        assertThat(events).hasSize(2);
    }

    private SignCommandHandler handler(InMemorySignRepository signs, List<SignAcceptedEvent> events) {
        MaterialRepository materials = new MaterialRepository() {
            @Override public void saveAll(List<Material> values) { }
            @Override public List<Material> listByTenantAndSignNo(String tenantCode, String signNo) { return List.of(); }
        };
        InteractionLogRepository audits = log -> { };
        SignCreationTransaction transaction = new SignCreationTransaction(signs, materials, audits);
        BusinessLock lock = new BusinessLock() { @Override public <T> T execute(String key, LockedOperation<T> action) { return action.run(); } };
        SignNumberGenerator numbers = () -> new SignNo("S%014d".formatted(signs.values.size() + 1));
        EventPublisher publisher = events::add;
        return new SignCommandHandler(signs, lock, numbers, publisher, transaction);
    }

    private static final class InMemorySignRepository implements SignRepository {
        private final List<Sign> values = new ArrayList<>();
        @Override public Optional<Sign> findByTenantAndExternalApplyNo(String tenant, String external) {
            return values.stream().filter(value -> value.tenantCode().equals(tenant) && value.externalApplyNo().equals(external)).findFirst();
        }
        @Override public Optional<Sign> findByTenantAndSignNo(String tenant, SignNo signNo) {
            return values.stream().filter(value -> value.tenantCode().equals(tenant) && value.signNo().equals(signNo)).findFirst();
        }
        @Override public Optional<SignNo> findHighestSignNo() { return values.stream().map(Sign::signNo).max((left, right) -> left.value().compareTo(right.value())); }
        @Override public List<Sign> findStalledBefore(Instant cutoff, int limit) { return List.of(); }
        @Override public void save(Sign sign) { values.add(sign); }
    }
}
