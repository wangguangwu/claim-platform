package com.claim.platform.sign.interfaces.tenant.core.pipeline;

import com.claim.platform.sign.application.command.SubmitIntakeCommand;
import com.claim.platform.sign.interfaces.tenant.core.context.TenantContext;
import com.claim.platform.sign.interfaces.tenant.core.context.TenantContextHolder;
import com.claim.platform.sign.interfaces.tenant.core.registry.TenantProtocolHandler;
import com.claim.platform.sign.interfaces.tenant.core.registry.TenantProtocolHandlerRegistry;
import com.claim.platform.sign.interfaces.web.exception.TenantContextMissingException;
import org.springframework.stereotype.Component;

/**
 * 默认转换流程：读取可信租户上下文、解析组件包，并按固定顺序执行解析、校验和转换。
 * 任一阶段失败立即终止，不返回半成品命令。
 *
 * @author wangguangwu
 */
@Component
public class DefaultIntakeConvertPipeline implements IntakeConvertPipeline {

    private final TenantProtocolHandlerRegistry registry;

    public DefaultIntakeConvertPipeline(TenantProtocolHandlerRegistry registry) {
        this.registry = registry;
    }

    @Override
    public SubmitIntakeCommand execute(String payload) {
        TenantContext context = TenantContextHolder.get()
                .orElseThrow(TenantContextMissingException::new);
        TenantProtocolHandler<?> handler = registry.get(context);
        return convert(handler, payload, context);
    }

    @SuppressWarnings("unchecked")
    private <T> SubmitIntakeCommand convert(TenantProtocolHandler<?> handler, String payload,
                                            TenantContext context) {
        TenantProtocolHandler<T> typedHandler = (TenantProtocolHandler<T>) handler;
        T request = typedHandler.parser().parse(payload);
        typedHandler.validator().validate(request);
        return typedHandler.converter().convert(request, context);
    }
}
