package com.claim.platform.intake.interfaces.web.advice;

import com.claim.platform.intake.application.exception.IntakeSubmissionInProgressException;
import com.claim.platform.intake.application.exception.SignNoSequenceExhaustedException;
import com.claim.platform.intake.application.exception.SignNoUnavailableException;
import com.claim.platform.intake.domain.exception.InvalidIntakeStatusException;
import com.claim.platform.intake.domain.exception.ConcurrentIntakeModificationException;
import com.claim.platform.intake.domain.exception.ClaimIntakeNotFoundException;
import com.claim.platform.common.web.ApiResult;
import com.claim.platform.intake.interfaces.tenant.core.exception.IntakeCodeMappingException;
import com.claim.platform.intake.interfaces.tenant.core.exception.IntakeConversionException;
import com.claim.platform.intake.interfaces.tenant.core.exception.IntakePayloadParseException;
import com.claim.platform.intake.interfaces.tenant.core.exception.IntakeRequestValidationException;
import com.claim.platform.intake.interfaces.tenant.core.exception.TenantProtocolNotSupportedException;
import com.claim.platform.intake.interfaces.web.context.GatewayRequestContextResolver;
import com.claim.platform.intake.interfaces.web.exception.TenantContextMissingException;
import com.claim.platform.intake.interfaces.web.exception.UntrustedCallerException;
import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

/**
 * 将 Claim Intake 的边界异常映射为稳定响应信封，绝不返回内部异常详情。
 *
 * @author wangguangwu
 */
@RestControllerAdvice
public class ClaimIntakeExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClaimIntakeExceptionHandler.class);

    /**
     * 返回请求参数校验失败，不记录原始敏感值。
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, HandlerMethodValidationException.class,
            IllegalArgumentException.class})
    public ResponseEntity<ApiResult<Void>> validation(Exception exception, HttpServletRequest request) {
        String traceId = traceId(request);
        String field = exception instanceof MethodArgumentNotValidException validation
                && !validation.getAllErrors().isEmpty()
                ? validation.getAllErrors().get(0).getObjectName() : "request";
        LOGGER.warn("claim intake request validation failed field={} traceId={}", field, traceId);
        return failure(HttpStatus.BAD_REQUEST, "REQUEST_VALIDATION_FAILED", "请求参数不合法", traceId);
    }

    /**
     * 返回未通过服务间调用鉴别的失败，避免接受伪造租户头。
     */
    @ExceptionHandler(UntrustedCallerException.class)
    public ResponseEntity<ApiResult<Void>> untrusted(UntrustedCallerException exception, HttpServletRequest request) {
        return failure(HttpStatus.UNAUTHORIZED, "UNTRUSTED_CALLER", "调用方未通过内部鉴别", traceId(request));
    }

    /**
     * 返回缺失可信租户上下文的失败。
     */
    @ExceptionHandler(TenantContextMissingException.class)
    public ResponseEntity<ApiResult<Void>> missingTenant(TenantContextMissingException exception, HttpServletRequest request) {
        return failure(HttpStatus.BAD_REQUEST, "TENANT_CONTEXT_MISSING", "缺少租户上下文", traceId(request));
    }

    /**
     * 返回租户报文结构非法，只记录字段路径和 traceId，不记录原始报文。
     */
    @ExceptionHandler(IntakePayloadParseException.class)
    public ResponseEntity<ApiResult<Void>> malformedPayload(IntakePayloadParseException exception,
                                                            HttpServletRequest request) {
        String traceId = traceId(request);
        LOGGER.warn("tenant intake parse failed fieldPath={} traceId={}", exception.fieldPath(), traceId);
        return failure(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "请求报文格式不合法", traceId);
    }

    /**
     * 返回公共/租户校验或编码映射失败，响应只包含固定文案。
     */
    @ExceptionHandler({IntakeRequestValidationException.class, IntakeCodeMappingException.class})
    public ResponseEntity<ApiResult<Void>> tenantRequestValidation(IntakeConversionException exception,
                                                                   HttpServletRequest request) {
        String traceId = traceId(request);
        LOGGER.warn("tenant intake {} failed fieldPath={} traceId={}",
                exception.getClass().getSimpleName(), exception.fieldPath(), traceId);
        return failure(HttpStatus.BAD_REQUEST, "REQUEST_VALIDATION_FAILED", "请求参数不合法", traceId);
    }

    /**
     * 返回当前租户与协议版本没有完整注册组件的失败，不做跨租户或跨版本回退。
     */
    @ExceptionHandler(TenantProtocolNotSupportedException.class)
    public ResponseEntity<ApiResult<Void>> tenantProtocolNotSupported(TenantProtocolNotSupportedException exception,
                                                                      HttpServletRequest request) {
        String traceId = traceId(request);
        LOGGER.warn("tenant protocol not supported tenantCode={} protocolVersion={} traceId={}",
                exception.tenantCode(), exception.protocolVersion(), traceId);
        return failure(HttpStatus.BAD_REQUEST, "TENANT_PROTOCOL_NOT_SUPPORTED", "租户协议不支持", traceId);
    }

    /**
     * 返回当前租户看不到申请的统一不存在响应。
     */
    @ExceptionHandler(ClaimIntakeNotFoundException.class)
    public ResponseEntity<ApiResult<Void>> notFound(ClaimIntakeNotFoundException exception, HttpServletRequest request) {
        return failure(HttpStatus.NOT_FOUND, "CLAIM_INTAKE_NOT_FOUND", "签收申请不存在", traceId(request));
    }

    /**
     * 返回状态机拒绝的冲突响应。
     */
    @ExceptionHandler(InvalidIntakeStatusException.class)
    public ResponseEntity<ApiResult<Void>> invalidStatus(InvalidIntakeStatusException exception,
                                                         HttpServletRequest request) {
        return failure(HttpStatus.CONFLICT, "INVALID_INTAKE_STATUS", "当前申请状态不允许该操作", traceId(request));
    }

    /**
     * 返回同一幂等键正在处理中的并发响应，调用方应使用相同幂等键退避重试。
     */
    @ExceptionHandler(IntakeSubmissionInProgressException.class)
    public ResponseEntity<ApiResult<Void>> submissionInProgress(IntakeSubmissionInProgressException exception,
                                                                HttpServletRequest request) {
        String traceId = traceId(request);
        LOGGER.warn("claim intake submission in progress traceId={} result=lock_timeout", traceId);
        return failure(HttpStatus.CONFLICT, "INTAKE_SUBMISSION_IN_PROGRESS", "申请正在处理中，请稍后重试", traceId);
    }

    /**
     * 返回乐观锁冲突，提示调用方重新读取申请后再决定重试。
     */
    @ExceptionHandler(ConcurrentIntakeModificationException.class)
    public ResponseEntity<ApiResult<Void>> concurrentModification(ConcurrentIntakeModificationException exception,
                                                                  HttpServletRequest request) {
        return failure(HttpStatus.CONFLICT, "CLAIM_INTAKE_CONCURRENT_MODIFICATION", "签收申请已被并发修改", traceId(request));
    }

    /**
     * 返回签收号服务暂时不可用，并提示调用方使用相同幂等键退避重试。
     */
    @ExceptionHandler(SignNoUnavailableException.class)
    public ResponseEntity<ApiResult<Void>> signNoUnavailable(SignNoUnavailableException exception,
                                                             HttpServletRequest request) {
        String traceId = traceId(request);
        LOGGER.warn("claim intake sign no unavailable reason={} traceId={}", exception.reason(), traceId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(HttpHeaders.RETRY_AFTER, "5")
                .body(ApiResult.failure("INTAKE_NUMBER_UNAVAILABLE", "签收号服务暂不可用，请稍后重试", traceId));
    }

    /**
     * 返回当日签收号耗尽，明确不可通过重试恢复。
     */
    @ExceptionHandler(SignNoSequenceExhaustedException.class)
    public ResponseEntity<ApiResult<Void>> signNoExhausted(SignNoSequenceExhaustedException exception,
                                                           HttpServletRequest request) {
        String traceId = traceId(request);
        LOGGER.error("claim intake sign no exhausted traceId={}", traceId);
        return failure(HttpStatus.SERVICE_UNAVAILABLE, "INTAKE_NUMBER_EXHAUSTED", "今日签收号已用尽，请联系平台", traceId);
    }

    /**
     * 隐藏未预期异常细节，并记录异常类别供 traceId 定位。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> unexpected(Exception exception, HttpServletRequest request) {
        String traceId = traceId(request);
        LOGGER.error("unexpected claim intake error type={} traceId={}",
                exception.getClass().getSimpleName(), traceId, exception);
        return failure(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "系统繁忙，请稍后重试", traceId);
    }

    private ResponseEntity<ApiResult<Void>> failure(HttpStatus status, String code, String message, String traceId) {
        return ResponseEntity.status(status).body(ApiResult.failure(code, message, traceId));
    }

    private String traceId(HttpServletRequest request) {
        Object value = request.getAttribute(GatewayRequestContextResolver.TRACE_ATTRIBUTE);
        return value instanceof String traceId ? traceId : UUID.randomUUID().toString();
    }
}
