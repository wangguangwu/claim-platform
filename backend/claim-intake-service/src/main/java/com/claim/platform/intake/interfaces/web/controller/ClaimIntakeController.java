package com.claim.platform.intake.interfaces.web.controller;

import com.claim.platform.common.web.ApiResult;
import com.claim.platform.intake.application.service.ClaimIntakeCommandService;
import com.claim.platform.intake.application.service.ClaimIntakeQueryService;
import com.claim.platform.intake.application.command.SubmitIntakeCommand;
import com.claim.platform.intake.application.command.UploadMaterialsCommand;
import com.claim.platform.intake.application.result.ClaimIntakeDetailResult;
import com.claim.platform.intake.application.result.ClaimIntakeResult;
import com.claim.platform.intake.domain.model.IntakeMaterial;
import com.claim.platform.intake.interfaces.web.ClaimIntakeWebContract;
import com.claim.platform.intake.interfaces.web.converter.PersonRequestConverter;
import com.claim.platform.intake.interfaces.web.context.GatewayRequestContext;
import com.claim.platform.intake.interfaces.web.context.GatewayRequestContextResolver;
import com.claim.platform.intake.interfaces.web.request.CreateIntakeRequest;
import com.claim.platform.intake.interfaces.web.request.UploadMaterialsRequest;
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
 * Claim Intake 的 HTTP 协议适配器，只负责输入校验、可信上下文提取、人员 JSON 到领域模型的边界转换与响应封装。
 * 领域层不直接解析人员 JSON，转换责任落在接口层的 {@link PersonRequestConverter}。
 *
 * @author wangguangwu
 */
@Validated
@RestController
@RequestMapping(ClaimIntakeWebContract.Paths.CLAIM_INTAKES)
public class ClaimIntakeController {

    private final GatewayRequestContextResolver contextResolver;
    private final ClaimIntakeCommandService commandService;
    private final ClaimIntakeQueryService queryService;
    private final ObjectMapper objectMapper;
    private final PersonRequestConverter personRequestConverter;

    public ClaimIntakeController(GatewayRequestContextResolver contextResolver,
                                 ClaimIntakeCommandService commandService,
                                 ClaimIntakeQueryService queryService, ObjectMapper objectMapper,
                                 PersonRequestConverter personRequestConverter) {
        this.contextResolver = contextResolver;
        this.commandService = commandService;
        this.queryService = queryService;
        this.objectMapper = objectMapper;
        this.personRequestConverter = personRequestConverter;
    }

    /**
     * 接收签收申请；同一外部申请编号回放既有签收事实。
     * 人员信息在边界内由 JSON 经 {@link PersonRequestConverter} 转为领域 Person 子类型。
     *
     * @return 创建或回放后的申请结果
     */
    @PostMapping
    public ResponseEntity<ApiResult<ClaimIntakeResult>> create(
            @Valid @RequestBody CreateIntakeRequest request, HttpServletRequest servletRequest) {
        GatewayRequestContext context = resolveContext(servletRequest);
        ClaimIntakeResult result = commandService.submit(new SubmitIntakeCommand(context.tenantCode(),
                context.traceId(), request.externalApplyNo(), request.applicantName(),
                personRequestConverter.toApplicant(request.applicantInfo()),
                personRequestConverter.toAccidentPerson(request.accidentPersonInfo()),
                personRequestConverter.toMainInsured(request.mainInsuredInfo()),
                personRequestConverter.toPayee(request.payeeInfo()), List.of(),
                toJson(request.extJson())));
        return ResponseEntity.ok(ApiResult.success(result, context.traceId()));
    }

    /**
     * 登记一批材料元数据；材料上传状态由聚合校验并在事务内更新。
     *
     * @return 材料登记后的申请结果
     */
    @PostMapping("/{signNo}/materials")
    public ResponseEntity<ApiResult<ClaimIntakeResult>> uploadMaterials(
            @PathVariable @NotBlank @Size(max = ClaimIntakeWebContract.Limits.SIGN_NO_LENGTH) String signNo,
            @Valid @RequestBody UploadMaterialsRequest request, HttpServletRequest servletRequest) {
        GatewayRequestContext context = resolveContext(servletRequest);
        List<IntakeMaterial> materials = request.materials().stream()
                .map(material -> new IntakeMaterial(request.batchNo(), material.sequence(), material.type(),
                        material.name(), material.path(), material.originalPath()))
                .toList();
        ClaimIntakeResult result = commandService.uploadMaterials(new UploadMaterialsCommand(context.tenantCode(),
                signNo, context.traceId(), materials));
        return ResponseEntity.ok(ApiResult.success(result, context.traceId()));
    }

    /**
     * 查询当前租户范围内的申请和材料，不暴露跨租户信息。
     *
     * @return 当前租户申请详情
     */
    @GetMapping("/{signNo}")
    public ResponseEntity<ApiResult<ClaimIntakeDetailResult>> get(
            @PathVariable @NotBlank @Size(max = ClaimIntakeWebContract.Limits.SIGN_NO_LENGTH) String signNo,
            HttpServletRequest servletRequest) {
        GatewayRequestContext context = resolveContext(servletRequest);
        ClaimIntakeDetailResult result = queryService.get(context.tenantCode(), signNo);
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
