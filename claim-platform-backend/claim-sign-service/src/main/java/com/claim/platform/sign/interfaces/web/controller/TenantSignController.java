package com.claim.platform.sign.interfaces.web.controller;

import com.claim.platform.common.web.ApiResult;
import com.claim.platform.sign.application.command.SignCommand;
import com.claim.platform.sign.application.result.SignResult;
import com.claim.platform.sign.application.handler.SignCommandHandler;
import com.claim.platform.sign.interfaces.tenant.core.context.TenantContext;
import com.claim.platform.sign.interfaces.tenant.core.context.TenantContextHolder;
import com.claim.platform.sign.interfaces.tenant.core.pipeline.SignConvertPipeline;
import com.claim.platform.sign.interfaces.web.SignWebContract;
import com.claim.platform.sign.interfaces.web.context.TenantSignContextResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 租户 Raw Payload 的 HTTP 入口：解析可信上下文、调用固定转换 Pipeline，再提交平台签收命令。
 * Holder 只在 Pipeline 调用期间持有，转换结束立即清理；本入口不生成签收编号、不写数据库、不发消息。
 *
 * @author wangguangwu
 */
@RestController
@RequestMapping(SignWebContract.Paths.TENANT_INTAKES)
public class TenantSignController {

    private final TenantSignContextResolver contextResolver;
    private final SignConvertPipeline convertPipeline;
    private final SignCommandHandler commandService;

    public TenantSignController(TenantSignContextResolver contextResolver,
                                  SignConvertPipeline convertPipeline,
                                  SignCommandHandler commandService) {
        this.contextResolver = contextResolver;
        this.convertPipeline = convertPipeline;
        this.commandService = commandService;
    }

    /**
     * 接收已完成验签和解密的租户报文，转换为平台命令并提交签收。
     * 幂等键沿用 Application 的 {@code tenantCode + externalApplyNo} 规则。
     *
     * @param payload 租户 Raw Payload
     * @param request 当前 HTTP 请求
     * @return 创建或回放后的签收结果
     */
    @PostMapping
    public ResponseEntity<ApiResult<SignResult>> receive(
            @RequestBody(required = false) String payload, HttpServletRequest request) {
        TenantContext context = contextResolver.resolve(request);
        SignCommand command;
        try {
            TenantContextHolder.set(context);
            command = convertPipeline.execute(payload);
        } finally {
            TenantContextHolder.clear();
        }
        SignResult result = commandService.submit(command);
        return ResponseEntity.ok(ApiResult.success(result, context.traceId()));
    }
}
