package com.claim.platform.intake.adapter.web;

import com.claim.platform.common.web.ApiResult;
import com.claim.platform.intake.application.ClaimIntakeCommandService;
import com.claim.platform.intake.application.ClaimIntakeQueryService;
import com.claim.platform.intake.application.command.SubmitIntakeCommand;
import com.claim.platform.intake.application.command.UploadMaterialsCommand;
import com.claim.platform.intake.application.result.ClaimIntakeDetailResult;
import com.claim.platform.intake.application.result.ClaimIntakeResult;
import com.claim.platform.intake.domain.model.IntakeMaterial;
import com.claim.platform.intake.domain.model.person.AccidentPerson;
import com.claim.platform.intake.domain.model.person.Applicant;
import com.claim.platform.intake.domain.model.person.MainInsured;
import com.claim.platform.intake.domain.model.person.Payee;
import com.claim.platform.intake.infrastructure.persistence.PersonConverter;
import com.claim.platform.intake.infrastructure.persistence.person.AccidentPersonInfoDO;
import com.claim.platform.intake.infrastructure.persistence.person.ApplicantInfoDO;
import com.claim.platform.intake.infrastructure.persistence.person.MainInsuredInfoDO;
import com.claim.platform.intake.infrastructure.persistence.person.PayeeInfoDO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.function.Function;
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
 * 领域层不直接解析人员 JSON，转换责任落在本适配器与 PersonConverter。
 *
 * @author wangguangwu
 */
@Validated
@RestController
@RequestMapping("/api/v1/claim-intakes")
public class ClaimIntakeController {

    private final GatewayRequestContextResolver contextResolver;
    private final ClaimIntakeCommandService commandService;
    private final ClaimIntakeQueryService queryService;
    private final ObjectMapper objectMapper;
    private final PersonConverter personConverter;

    public ClaimIntakeController(GatewayRequestContextResolver contextResolver,
                                 ClaimIntakeCommandService commandService,
                                 ClaimIntakeQueryService queryService, ObjectMapper objectMapper,
                                 PersonConverter personConverter) {
        this.contextResolver = contextResolver;
        this.commandService = commandService;
        this.queryService = queryService;
        this.objectMapper = objectMapper;
        this.personConverter = personConverter;
    }

    /**
     * 接收签收申请；同一外部申请编号回放既有签收事实。
     * 人员信息在边界内由 JSON 经 PersonConverter 转为领域 Person 子类型。
     *
     * @return 创建或回放后的申请结果
     */
    @PostMapping
    public ResponseEntity<ApiResult<ClaimIntakeResult>> create(
            @Valid @RequestBody CreateIntakeRequest request, HttpServletRequest servletRequest) {
        GatewayRequestContext context = resolveContext(servletRequest);
        ClaimIntakeResult result = commandService.submit(new SubmitIntakeCommand(context.tenantCode(),
                context.traceId(), request.externalApplyNo(), request.applicantName(),
                toPerson(request.applicantInfo(), ApplicantInfoDO.class, personConverter::toApplicant),
                toPerson(request.accidentPersonInfo(), AccidentPersonInfoDO.class, personConverter::toAccidentPerson),
                toPerson(request.mainInsuredInfo(), MainInsuredInfoDO.class, personConverter::toMainInsured),
                toPerson(request.payeeInfo(), PayeeInfoDO.class, personConverter::toPayee),
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
            @PathVariable @NotBlank @Size(max = 64) String signNo,
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
            @PathVariable @NotBlank @Size(max = 64) String signNo, HttpServletRequest servletRequest) {
        GatewayRequestContext context = resolveContext(servletRequest);
        ClaimIntakeDetailResult result = queryService.get(context.tenantCode(), signNo);
        return ResponseEntity.ok(ApiResult.success(result, context.traceId()));
    }

    /**
     * 将入站人员 JSON 节点先反序列化为对应持久化对象，再经 PersonConverter 转为领域 Person 子类型。
     *
     * @param node 入站 JSON 节点，可为 null
     * @param doType 目标持久化对象类型
     * @param convert PersonConverter 提供的领域转换函数
     * @param <D> 持久化对象类型
     * @param <P> 领域 Person 子类型
     * @return 领域 Person，节点为空时返回 null
     */
    private <D, P> P toPerson(JsonNode node, Class<D> doType, Function<D, P> convert) {
        if (node == null || node.isNull()) {
            return null;
        }
        try {
            D info = objectMapper.treeToValue(node, doType);
            return convert.apply(info);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("人员 JSON 内容不合法", exception);
        }
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
