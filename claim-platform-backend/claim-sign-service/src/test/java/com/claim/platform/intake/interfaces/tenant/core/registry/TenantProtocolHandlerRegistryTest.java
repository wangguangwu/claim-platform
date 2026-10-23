package com.claim.platform.intake.interfaces.tenant.core.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.claim.platform.intake.interfaces.tenant.core.context.TenantContext;
import com.claim.platform.intake.interfaces.tenant.core.exception.TenantProtocolNotSupportedException;
import com.claim.platform.intake.interfaces.tenant.support.StubTenantProtocolHandler;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 验证注册表的精确匹配、版本并存和启动失败规则。
 *
 * @author wangguangwu
 */
class TenantProtocolHandlerRegistryTest {

    @Test
    void shouldResolveExactTenantAndProtocolVersion() {
        TenantProtocolHandler<String> v1 = handler("tenant-a", "v1");
        TenantProtocolHandler<String> v2 = handler("tenant-a", "v2");
        TenantProtocolHandlerRegistry registry = registry(v1, v2);

        assertThat(registry.get(context("tenant-a", "v1"))).isSameAs(v1);
        assertThat(registry.get(context("tenant-a", "v2"))).isSameAs(v2);
    }

    @Test
    void shouldKeepDifferentTenantsIsolated() {
        TenantProtocolHandler<String> tenantA = handler("tenant-a", "v1");
        TenantProtocolHandler<String> tenantB = handler("tenant-b", "v1");
        TenantProtocolHandlerRegistry registry = registry(tenantA, tenantB);

        assertThat(registry.get(context("tenant-a", "v1"))).isSameAs(tenantA);
        assertThat(registry.get(context("tenant-b", "v1"))).isSameAs(tenantB);
    }

    @Test
    void shouldRejectDuplicateProtocolRegistration() {
        TenantProtocolHandler<String> first = handler("tenant-a", "v1");
        TenantProtocolHandler<String> duplicate = handler("tenant-a", "v1");

        assertThatThrownBy(() -> registry(first, duplicate))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("重复注册租户协议");
    }

    @Test
    void shouldRejectIncompleteProtocolHandler() {
        TenantProtocolHandler<String> incomplete = new StubTenantProtocolHandler<>(
                new TenantProtocolKey("tenant-a", "v1"), null, request -> {
        }, (request, context) -> null);

        assertThatThrownBy(() -> registry(incomplete))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("组件不完整");
    }

    @Test
    void shouldRejectNullProtocolHandler() {
        List<TenantProtocolHandler<?>> handlers = Arrays.asList(handler("tenant-a", "v1"), null);

        assertThatThrownBy(() -> new TenantProtocolHandlerRegistry(handlers))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("组件不完整");
    }

    @Test
    void shouldRejectUnregisteredProtocolWithoutFallback() {
        TenantProtocolHandlerRegistry registry = registry(handler("tenant-a", "v1"));

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

    private TenantProtocolHandler<String> handler(String tenantCode, String protocolVersion) {
        return new StubTenantProtocolHandler<>(new TenantProtocolKey(tenantCode, protocolVersion),
                payload -> payload, request -> {
        }, (request, context) -> null);
    }

    private TenantProtocolHandlerRegistry registry(TenantProtocolHandler<?>... handlers) {
        return new TenantProtocolHandlerRegistry(List.of(handlers));
    }

    private TenantContext context(String tenantCode, String protocolVersion) {
        return new TenantContext(tenantCode, protocolVersion, "trace-001");
    }
}
