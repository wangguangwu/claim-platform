package com.claim.platform.sign.interfaces.web.advice;

import com.claim.platform.sign.application.exception.SignSubmissionInProgressException;
import com.claim.platform.sign.application.exception.SignNoSequenceExhaustedException;
import com.claim.platform.sign.application.exception.SignNoUnavailableException;
import com.claim.platform.sign.domain.exception.InvalidSignStatusException;
import com.claim.platform.sign.domain.exception.ConcurrentSignModificationException;
import com.claim.platform.sign.domain.exception.SignNotFoundException;
import com.claim.platform.common.web.ApiResult;
import com.claim.platform.sign.interfaces.web.SignWebContract;
import com.claim.platform.sign.interfaces.web.context.GatewayRequestContextResolver;
import com.claim.platform.sign.interfaces.web.exception.TenantContextMissingException;
import com.claim.platform.sign.interfaces.web.exception.UntrustedCallerException;
import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

/**
 * 将理赔签收的边界异常映射为稳定响应信封，绝不返回内部异常详情。
 *
 * @author wangguangwu
 */
@RestControllerAdvice
public class SignExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SignExceptionHandler.class);

    /**
     * 返回请求参数校验失败，不记录原始敏感值。
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, HandlerMethodValidationException.class,
            IllegalArgumentException.class})
    public ResponseEntity<ApiResult<Void>> validation(Exception exception, HttpServletRequest request) {
        String traceId = traceId(request);
        String field = exception instanceof MethodArgumentNotValidException validation
                && !validation.getAllErrors().isEmpty()
                ? validation.getAllErrors().get(0).getObjectName() : SignWebContract.DEFAULT_FIELD;
        LOGGER.warn("claim sign request validation failed field={} traceId={}", field, traceId);
        return failure(SignErrorCode.REQUEST_VALIDATION_FAILED, traceId);
    }

    /**
     * 返回未通过服务间调用鉴别的失败，避免接受伪造租户头。
     */
    @ExceptionHandler(UntrustedCallerException.class)
    public ResponseEntity<ApiResult<Void>> untrusted(UntrustedCallerException exception, HttpServletRequest request) {
        return failure(SignErrorCode.UNTRUSTED_CALLER, traceId(request));
    }

    /**
     * 返回缺失可信租户上下文的失败。
     */
    @ExceptionHandler(TenantContextMissingException.class)
    public ResponseEntity<ApiResult<Void>> missingTenant(TenantContextMissingException exception, HttpServletRequest request) {
        return failure(SignErrorCode.TENANT_CONTEXT_MISSING, traceId(request));
    }

    /**
     * 返回当前租户看不到申请的统一不存在响应。
     */
    @ExceptionHandler(SignNotFoundException.class)
    public ResponseEntity<ApiResult<Void>> notFound(SignNotFoundException exception, HttpServletRequest request) {
        return failure(SignErrorCode.CLAIM_INTAKE_NOT_FOUND, traceId(request));
    }

    /**
     * 返回状态机拒绝的冲突响应。
     */
    @ExceptionHandler(InvalidSignStatusException.class)
    public ResponseEntity<ApiResult<Void>> invalidStatus(InvalidSignStatusException exception,
                                                         HttpServletRequest request) {
        return failure(SignErrorCode.INVALID_INTAKE_STATUS, traceId(request));
    }

    /**
     * 返回同一幂等键正在处理中的并发响应，调用方应使用相同幂等键退避重试。
     */
    @ExceptionHandler(SignSubmissionInProgressException.class)
    public ResponseEntity<ApiResult<Void>> submissionInProgress(SignSubmissionInProgressException exception,
                                                                HttpServletRequest request) {
        String traceId = traceId(request);
        LOGGER.warn("claim sign submission in progress traceId={} result=lock_timeout", traceId);
        return failure(SignErrorCode.INTAKE_SUBMISSION_IN_PROGRESS, traceId);
    }

    /**
     * 返回乐观锁冲突，提示调用方重新读取申请后再决定重试。
     */
    @ExceptionHandler(ConcurrentSignModificationException.class)
    public ResponseEntity<ApiResult<Void>> concurrentModification(ConcurrentSignModificationException exception,
                                                                  HttpServletRequest request) {
        return failure(SignErrorCode.CLAIM_INTAKE_CONCURRENT_MODIFICATION, traceId(request));
    }

    /**
     * 返回签收号服务暂时不可用，并提示调用方使用相同幂等键退避重试。
     */
    @ExceptionHandler(SignNoUnavailableException.class)
    public ResponseEntity<ApiResult<Void>> signNoUnavailable(SignNoUnavailableException exception,
                                                             HttpServletRequest request) {
        String traceId = traceId(request);
        LOGGER.warn("claim sign no unavailable reason={} traceId={}", exception.reason(), traceId);
        return failure(SignErrorCode.INTAKE_NUMBER_UNAVAILABLE, traceId);
    }

    /**
     * 返回当日签收号耗尽，明确不可通过重试恢复。
     */
    @ExceptionHandler(SignNoSequenceExhaustedException.class)
    public ResponseEntity<ApiResult<Void>> signNoExhausted(SignNoSequenceExhaustedException exception,
                                                           HttpServletRequest request) {
        String traceId = traceId(request);
        LOGGER.error("claim sign no exhausted traceId={}", traceId);
        return failure(SignErrorCode.INTAKE_NUMBER_EXHAUSTED, traceId);
    }

    /**
     * 隐藏未预期异常细节，并记录异常类别供 traceId 定位。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> unexpected(Exception exception, HttpServletRequest request) {
        String traceId = traceId(request);
        LOGGER.error("unexpected claim sign error type={} traceId={}",
                exception.getClass().getSimpleName(), traceId, exception);
        return failure(SignErrorCode.INTERNAL_ERROR, traceId);
    }

    private ResponseEntity<ApiResult<Void>> failure(SignErrorCode errorCode, String traceId) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(errorCode.status());
        if (errorCode.retryAfterSeconds() != null) {
            builder.header(HttpHeaders.RETRY_AFTER, String.valueOf(errorCode.retryAfterSeconds()));
        }
        return builder.body(ApiResult.failure(errorCode.code(), errorCode.message(), traceId));
    }

    private String traceId(HttpServletRequest request) {
        Object value = request.getAttribute(GatewayRequestContextResolver.TRACE_ATTRIBUTE);
        return value instanceof String traceId ? traceId : UUID.randomUUID().toString();
    }
}
