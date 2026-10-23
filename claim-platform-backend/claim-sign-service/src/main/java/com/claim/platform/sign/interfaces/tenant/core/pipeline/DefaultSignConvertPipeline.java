package com.claim.platform.sign.interfaces.tenant.core.pipeline;

import com.claim.platform.sign.application.command.SignCommand;
import com.claim.platform.sign.interfaces.tenant.core.context.TenantContext;
import com.claim.platform.sign.interfaces.tenant.core.context.TenantContextHolder;
import com.claim.platform.sign.interfaces.tenant.core.registry.TenantProtocolAdapter;
import com.claim.platform.sign.interfaces.tenant.core.registry.TenantProtocolAdapterRegistry;
import com.claim.platform.sign.interfaces.web.exception.TenantContextMissingException;
import org.springframework.stereotype.Component;

/**
 * 默认转换流程：读取可信租户上下文、解析组件包，并按固定顺序执行解析、校验和转换。
 * 任一阶段失败立即终止，不返回半成品命令。
 *
 * @author wangguangwu
 */
@Component
public class DefaultSignConvertPipeline implements SignConvertPipeline {

    private final TenantProtocolAdapterRegistry registry;

    public DefaultSignConvertPipeline(TenantProtocolAdapterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public SignCommand execute(String payload) {
        TenantContext context = TenantContextHolder.get()
                .orElseThrow(TenantContextMissingException::new);
        TenantProtocolAdapter<?> handler = registry.get(context);
        return convert(handler, payload, context);
    }

    @SuppressWarnings("unchecked")
    private <T> SignCommand convert(TenantProtocolAdapter<?> handler, String payload,
                                            TenantContext context) {
        TenantProtocolAdapter<T> typedHandler = (TenantProtocolAdapter<T>) handler;
        T request = typedHandler.parser().parse(payload);
        typedHandler.validator().validate(request);
        return typedHandler.converter().convert(request, context);
    }
}
