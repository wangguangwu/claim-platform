package com.claim.platform.intake.interfaces.web.context;

import com.claim.platform.intake.interfaces.web.exception.TenantContextMissingException;
import com.claim.platform.intake.interfaces.web.exception.UntrustedCallerException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 在服务边界校验 Gateway 的内部凭证并提取可信租户和 Trace 上下文。
 * 本地凭证来自环境变量，生产环境必须由受控密钥管理系统注入。
 *
 * @author wangguangwu
 */
@Component
public class GatewayRequestContextResolver {

    public static final String TRACE_ATTRIBUTE = GatewayRequestContextResolver.class.getName() + ".traceId";
    private final byte[] serviceToken;

    public GatewayRequestContextResolver(@Value("${claim.internal.service-token}") String serviceToken) {
        this.serviceToken = serviceToken.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 校验调用方后返回可信上下文；请求体从不参与租户判定。
     *
     * @throws UntrustedCallerException      服务间调用凭证不匹配
     * @throws TenantContextMissingException 未提供 Gateway 解析后的租户
     */
    public GatewayRequestContext resolve(HttpServletRequest request) {
        String providedToken = request.getHeader("X-Claim-Service-Token");
        if (!StringUtils.hasText(providedToken) || !MessageDigest.isEqual(serviceToken,
                providedToken.getBytes(StandardCharsets.UTF_8))) {
            throw new UntrustedCallerException();
        }
        String tenantCode = request.getHeader("X-Claim-Tenant");
        if (!StringUtils.hasText(tenantCode) || !tenantCode.matches("[A-Za-z0-9_-]{1,64}")) {
            throw new TenantContextMissingException();
        }
        String traceId = request.getHeader("X-Trace-Id");
        if (!StringUtils.hasText(traceId)) {
            traceId = UUID.randomUUID().toString();
        }
        request.setAttribute(TRACE_ATTRIBUTE, traceId);
        return new GatewayRequestContext(tenantCode, traceId);
    }
}
