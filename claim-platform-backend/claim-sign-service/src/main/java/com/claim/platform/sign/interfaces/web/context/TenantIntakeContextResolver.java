package com.claim.platform.sign.interfaces.web.context;

import com.claim.platform.sign.interfaces.tenant.core.context.TenantContext;
import com.claim.platform.sign.interfaces.web.ClaimIntakeWebContract;
import com.claim.platform.sign.interfaces.web.exception.TenantContextMissingException;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 在租户报文入口组合可信租户、协议版本和 Trace 上下文。
 * 协议版本只来自 Gateway 透传头，不接受请求体、查询参数或路径中的值，也不提供缺省版本。
 *
 * @author wangguangwu
 */
@Component
public class TenantIntakeContextResolver {

    private final GatewayRequestContextResolver gatewayContextResolver;

    public TenantIntakeContextResolver(GatewayRequestContextResolver gatewayContextResolver) {
        this.gatewayContextResolver = gatewayContextResolver;
    }

    /**
     * 校验服务间凭证并解析当前请求的不可变租户上下文。
     *
     * @param request 当前 HTTP 请求
     * @return 可信租户上下文
     * @throws TenantContextMissingException 缺少租户或协议版本头
     * @throws IllegalArgumentException      协议版本头格式非法
     */
    public TenantContext resolve(HttpServletRequest request) {
        GatewayRequestContext gatewayContext = gatewayContextResolver.resolve(request);
        String protocolVersion = request.getHeader(ClaimIntakeWebContract.Headers.PROTOCOL_VERSION);
        if (!StringUtils.hasText(protocolVersion)) {
            throw new TenantContextMissingException();
        }
        if (!ClaimIntakeWebContract.Patterns.PROTOCOL_VERSION.matcher(protocolVersion).matches()) {
            throw new IllegalArgumentException("协议版本头格式非法");
        }
        return new TenantContext(gatewayContext.tenantCode(), protocolVersion, gatewayContext.traceId());
    }
}
