package com.claim.platform.application.adapter.web;

import com.claim.platform.application.usecase.SignApplicationCommandService;
import com.claim.platform.application.usecase.SignApplicationQueryService;
import com.claim.platform.application.usecase.command.SubmitSignApplicationCommand;
import com.claim.platform.application.usecase.command.UploadMaterialsCommand;
import com.claim.platform.application.usecase.result.SignApplicationDetailResult;
import com.claim.platform.application.usecase.result.SignApplicationResult;
import com.claim.platform.application.domain.model.SignMaterial;
import com.claim.platform.common.web.ApiResult;
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
 * Application Intake 的 HTTP 协议适配器，只负责输入校验、可信上下文提取与响应封装。
 *
 * @author wangguangwu
 */
@Validated
@RestController
@RequestMapping("/api/v1/sign-applications")
public class SignApplicationController {

    private final GatewayRequestContextResolver contextResolver;
    private final SignApplicationCommandService commandService;
    private final SignApplicationQueryService queryService;
    private final ObjectMapper objectMapper;

    public SignApplicationController(GatewayRequestContextResolver contextResolver,
                                     SignApplicationCommandService commandService,
                                     SignApplicationQueryService queryService, ObjectMapper objectMapper) {
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
    public ResponseEntity<ApiResult<SignApplicationResult>> create(
            @Valid @RequestBody CreateSignApplicationRequest request, HttpServletRequest servletRequest) {
        GatewayRequestContext context = resolveContext(servletRequest);
        SignApplicationResult result = commandService.submit(new SubmitSignApplicationCommand(context.tenantCode(),
                context.traceId(), request.externalApplyNo(), request.applicantName(),
                toJson(request.applicantInfo()), toJson(request.accidentPersonInfo()), toJson(request.mainInsuredInfo()),
                toJson(request.payeeInfo()), toJson(request.extJson())));
        return ResponseEntity.ok(ApiResult.success(result, context.traceId()));
    }

    /**
     * 登记一批材料元数据；材料上传状态由聚合校验并在事务内更新。
     *
     * @return 材料登记后的申请结果
     */
    @PostMapping("/{signNo}/materials")
    public ResponseEntity<ApiResult<SignApplicationResult>> uploadMaterials(
            @PathVariable @NotBlank @Size(max = 64) String signNo,
            @Valid @RequestBody UploadMaterialsRequest request, HttpServletRequest servletRequest) {
        GatewayRequestContext context = resolveContext(servletRequest);
        List<SignMaterial> materials = request.materials().stream().map(material -> new SignMaterial(request.batchNo(),
                material.sequence(), material.type(), material.name(), material.path(), material.originalPath())).toList();
        SignApplicationResult result = commandService.uploadMaterials(new UploadMaterialsCommand(context.tenantCode(),
                signNo, context.traceId(), materials));
        return ResponseEntity.ok(ApiResult.success(result, context.traceId()));
    }

    /**
     * 查询当前租户范围内的申请和材料，不暴露跨租户信息。
     *
     * @return 当前租户申请详情
     */
    @GetMapping("/{signNo}")
    public ResponseEntity<ApiResult<SignApplicationDetailResult>> get(
            @PathVariable @NotBlank @Size(max = 64) String signNo, HttpServletRequest servletRequest) {
        GatewayRequestContext context = resolveContext(servletRequest);
        SignApplicationDetailResult result = queryService.get(context.tenantCode(), signNo);
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
