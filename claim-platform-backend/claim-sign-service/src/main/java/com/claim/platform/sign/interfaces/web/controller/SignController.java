package com.claim.platform.sign.interfaces.web.controller;

import com.claim.platform.common.web.ApiResult;
import com.claim.platform.sign.application.handler.SignCommandHandler;
import com.claim.platform.sign.application.service.SignQueryService;
import com.claim.platform.sign.application.command.SignCommand;
import com.claim.platform.sign.application.command.UploadMaterialCommand;
import com.claim.platform.sign.application.result.SignDetailResult;
import com.claim.platform.sign.application.result.SignResult;
import com.claim.platform.sign.domain.model.Material;
import com.claim.platform.sign.interfaces.web.SignWebContract;
import com.claim.platform.sign.interfaces.web.context.GatewayRequestContext;
import com.claim.platform.sign.interfaces.web.context.GatewayRequestContextResolver;
import com.claim.platform.sign.interfaces.web.request.SignRequest;
import com.claim.platform.sign.interfaces.web.request.UploadMaterialRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 理赔签收的 HTTP 协议适配器，只负责输入校验、可信上下文提取与响应封装。
 * 人员明细不在边界内建模，随 {@code extJson} 原样传入应用层。
 *
 * @author wangguangwu
 */
@Validated
@RestController
@RequestMapping(SignWebContract.Paths.CLAIM_INTAKES)
public class SignController {

    private final GatewayRequestContextResolver contextResolver;
    private final SignCommandHandler commandService;
    private final SignQueryService queryService;
    private final ObjectMapper objectMapper;

    public SignController(GatewayRequestContextResolver contextResolver,
                                 SignCommandHandler commandService,
                                 SignQueryService queryService, ObjectMapper objectMapper) {
        this.contextResolver = contextResolver;
        this.commandService = commandService;
        this.queryService = queryService;
        this.objectMapper = objectMapper;
    }

    /**
     * 接收签收申请；同一外部申请编号回放既有签收事实。
     *
     * @return 创建或回放后的申请结果
     */
    @PostMapping
    public ResponseEntity<ApiResult<SignResult>> create(
            @Valid @RequestBody SignRequest request, HttpServletRequest servletRequest) {
        GatewayRequestContext context = resolveContext(servletRequest);
        SignResult result = commandService.submit(new SignCommand(context.tenantCode(),
                context.traceId(), request.externalApplyNo(), request.applicantName(), List.of(),
                toJson(request.extJson())));
        return ResponseEntity.ok(ApiResult.success(result, context.traceId()));
    }

    /**
     * 登记一批材料元数据；材料上传状态由聚合校验并在事务内更新。
     *
     * @return 材料登记后的申请结果
     */
    @PostMapping("/{signNo}/materials")
    public ResponseEntity<ApiResult<SignResult>> uploadMaterials(
            @PathVariable @NotBlank @Size(max = SignWebContract.Limits.SIGN_NO_LENGTH) String signNo,
            @Valid @RequestBody UploadMaterialRequest request, HttpServletRequest servletRequest) {
        GatewayRequestContext context = resolveContext(servletRequest);
        List<Material> materials = request.materials().stream()
                .map(material -> new Material(request.batchNo(), material.sequence(), material.type(),
                        material.name(), material.path(), material.originalPath()))
                .toList();
        SignResult result = commandService.uploadMaterials(new UploadMaterialCommand(context.tenantCode(),
                signNo, context.traceId(), materials));
        return ResponseEntity.ok(ApiResult.success(result, context.traceId()));
    }

    /**
     * 查询当前租户范围内的申请和材料，不暴露跨租户信息。
     *
     * @return 当前租户申请详情
     */
    @GetMapping("/{signNo}")
    public ResponseEntity<ApiResult<SignDetailResult>> get(
            @PathVariable @NotBlank @Size(max = SignWebContract.Limits.SIGN_NO_LENGTH) String signNo,
            HttpServletRequest servletRequest) {
        GatewayRequestContext context = resolveContext(servletRequest);
        SignDetailResult result = queryService.get(context.tenantCode(), signNo);
        return ResponseEntity.ok(ApiResult.success(result, context.traceId()));
    }

    private String toJson(JsonNode value) {
        if (value == null || value.isNull()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("JSON 内容不合法", exception);
        }
    }

    private GatewayRequestContext resolveContext(HttpServletRequest request) {
        GatewayRequestContext context = contextResolver.resolve(request);
        request.setAttribute(GatewayRequestContextResolver.TRACE_ATTRIBUTE, context.traceId());
        return context;
    }
}
