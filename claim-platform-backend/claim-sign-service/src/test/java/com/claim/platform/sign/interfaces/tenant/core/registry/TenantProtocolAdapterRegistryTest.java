package com.claim.platform.sign.interfaces.tenant.core.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.claim.platform.sign.interfaces.tenant.core.context.TenantContext;
import com.claim.platform.sign.interfaces.tenant.core.exception.TenantProtocolNotSupportedException;
import com.claim.platform.sign.interfaces.tenant.support.StubTenantProtocolAdapter;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 验证注册表的精确匹配、版本并存和启动失败规则。
 *
 * @author wangguangwu
 */
class TenantProtocolAdapterRegistryTest {

    @Test
    void shouldResolveExactTenantAndProtocolVersion() {
        TenantProtocolAdapter<String> v1 = handler("tenant-a", "v1");
        TenantProtocolAdapter<String> v2 = handler("tenant-a", "v2");
        TenantProtocolAdapterRegistry registry = registry(v1, v2);

        assertThat(registry.get(context("tenant-a", "v1"))).isSameAs(v1);
        assertThat(registry.get(context("tenant-a", "v2"))).isSameAs(v2);
    }

    @Test
    void shouldKeepDifferentTenantsIsolated() {
        TenantProtocolAdapter<String> tenantA = handler("tenant-a", "v1");
        TenantProtocolAdapter<String> tenantB = handler("tenant-b", "v1");
        TenantProtocolAdapterRegistry registry = registry(tenantA, tenantB);

        assertThat(registry.get(context("tenant-a", "v1"))).isSameAs(tenantA);
        assertThat(registry.get(context("tenant-b", "v1"))).isSameAs(tenantB);
    }

    @Test
    void shouldRejectDuplicateProtocolRegistration() {
        TenantProtocolAdapter<String> first = handler("tenant-a", "v1");
        TenantProtocolAdapter<String> duplicate = handler("tenant-a", "v1");

        assertThatThrownBy(() -> registry(first, duplicate))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("重复注册租户协议");
    }

    @Test
    void shouldRejectIncompleteProtocolHandler() {
        TenantProtocolAdapter<String> incomplete = new StubTenantProtocolAdapter<>(
                new TenantProtocolKey("tenant-a", "v1"), null, request -> {
        }, (request, context) -> null);

        assertThatThrownBy(() -> registry(incomplete))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("组件不完整");
    }

    @Test
    void shouldRejectNullProtocolHandler() {
        List<TenantProtocolAdapter<?>> handlers = Arrays.asList(handler("tenant-a", "v1"), null);

        assertThatThrownBy(() -> new TenantProtocolAdapterRegistry(handlers))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("组件不完整");
    }

    @Test
    void shouldRejectUnregisteredProtocolWithoutFallback() {
        TenantProtocolAdapterRegistry registry = registry(handler("tenant-a", "v1"));

        TenantProtocolNotSupportedException exception = catchThrowableOfType(
                () -> registry.get(context("tenant-a", "v2")), TenantProtocolNotSupportedException.class);

        assertThat(exception.tenantCode()).isEqualTo("tenant-a");
        assertThat(exception.protocolVersion()).isEqualTo("v2");
    }

    @Test
    void shouldRejectInvalidProtocolKey() {
        assertThatThrownBy(() -> new TenantProtocolKey("tenant a", "v1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenantCode");
        assertThatThrownBy(() -> new TenantProtocolKey("tenant-a", "v 1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("protocolVersion");
    }

    private TenantProtocolAdapter<String> handler(String tenantCode, String protocolVersion) {
        return new StubTenantProtocolAdapter<>(new TenantProtocolKey(tenantCode, protocolVersion),
                payload -> payload, request -> {
        }, (request, context) -> null);
    }

    private TenantProtocolAdapterRegistry registry(TenantProtocolAdapter<?>... handlers) {
        return new TenantProtocolAdapterRegistry(List.of(handlers));
    }

    private TenantContext context(String tenantCode, String protocolVersion) {
        return new TenantContext(tenantCode, protocolVersion, "trace-001");
    }
}
