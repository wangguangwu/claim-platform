package com.claim.platform.intake.interfaces.tenant.core.registry;

import com.claim.platform.intake.interfaces.tenant.core.context.TenantContext;
import com.claim.platform.intake.interfaces.tenant.core.exception.TenantProtocolNotSupportedException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 在启动时收集全部 {@link TenantProtocolHandler} 并构建不可变注册表。
 * 只做 {@code tenantCode + protocolVersion} 精确匹配，不提供默认租户、默认版本或跨版本回退。
 *
 * @author wangguangwu
 */
@Component
public class TenantProtocolHandlerRegistry {

    private final Map<TenantProtocolKey, TenantProtocolHandler<?>> handlers;

    public TenantProtocolHandlerRegistry(List<TenantProtocolHandler<?>> handlers) {
        Map<TenantProtocolKey, TenantProtocolHandler<?>> registered = new LinkedHashMap<>();
        for (TenantProtocolHandler<?> handler : handlers) {
            validateComplete(handler);
            TenantProtocolHandler<?> previous = registered.putIfAbsent(handler.key(), handler);
            if (previous != null) {
                throw new IllegalStateException("重复注册租户协议，tenantCode=" + handler.key().tenantCode()
                        + "，protocolVersion=" + handler.key().protocolVersion());
            }
        }
        this.handlers = Map.copyOf(registered);
    }

    /**
     * 按可信租户上下文精确解析组件包。
     *
     * @param context 可信租户上下文
     * @return 已注册的组件包
     * @throws TenantProtocolNotSupportedException 当前租户与协议版本未完整注册
     */
    public TenantProtocolHandler<?> get(TenantContext context) {
        TenantProtocolKey key = new TenantProtocolKey(context.tenantCode(), context.protocolVersion());
        TenantProtocolHandler<?> handler = handlers.get(key);
        if (handler == null) {
            throw new TenantProtocolNotSupportedException(context.tenantCode(), context.protocolVersion());
        }
        return handler;
    }

    private void validateComplete(TenantProtocolHandler<?> handler) {
        if (handler == null || handler.key() == null || handler.parser() == null
                || handler.validator() == null || handler.converter() == null) {
            throw new IllegalStateException("租户协议组件不完整");
        }
    }
}
