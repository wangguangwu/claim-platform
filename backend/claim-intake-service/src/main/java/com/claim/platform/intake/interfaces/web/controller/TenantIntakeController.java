package com.claim.platform.intake.interfaces.web.controller;

import com.claim.platform.common.web.ApiResult;
import com.claim.platform.intake.application.command.SubmitIntakeCommand;
import com.claim.platform.intake.application.result.ClaimIntakeResult;
import com.claim.platform.intake.application.service.ClaimIntakeCommandService;
import com.claim.platform.intake.interfaces.tenant.core.context.TenantContext;
import com.claim.platform.intake.interfaces.tenant.core.context.TenantContextHolder;
import com.claim.platform.intake.interfaces.tenant.core.pipeline.IntakeConvertPipeline;
import com.claim.platform.intake.interfaces.web.context.TenantIntakeContextResolver;
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
@RequestMapping("/api/v1/tenant-intakes")
public class TenantIntakeController {

    private final TenantIntakeContextResolver contextResolver;
    private final IntakeConvertPipeline convertPipeline;
    private final ClaimIntakeCommandService commandService;

    public TenantIntakeController(TenantIntakeContextResolver contextResolver,
                                  IntakeConvertPipeline convertPipeline,
                                  ClaimIntakeCommandService commandService) {
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
    public ResponseEntity<ApiResult<ClaimIntakeResult>> receive(
            @RequestBody(required = false) String payload, HttpServletRequest request) {
        TenantContext context = contextResolver.resolve(request);
        SubmitIntakeCommand command;
        try {
            TenantContextHolder.set(context);
            command = convertPipeline.execute(payload);
        } finally {
            TenantContextHolder.clear();
        }
        ClaimIntakeResult result = commandService.submit(command);
        return ResponseEntity.ok(ApiResult.success(result, context.traceId()));
    }
}
