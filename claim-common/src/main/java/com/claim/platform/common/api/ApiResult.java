package com.claim.platform.common.api;

/**
 * Stable, domain-neutral HTTP response envelope shared by platform services.
 * Failure responses never carry internal exception details.
 *
 * @param <T> successful response payload type
 * @author wangguangwu
 */
public record ApiResult<T>(boolean success, String code, String message, String traceId, T data) {

    /** Creates a successful response with the stable OK code. */
    public static <T> ApiResult<T> success(T data, String traceId) {
        return new ApiResult<>(true, "OK", "成功", traceId, data);
    }

    /** Creates a safe failure response without a payload. */
    public static <T> ApiResult<T> failure(String code, String message, String traceId) {
        return new ApiResult<>(false, code, message, traceId, null);
    }
}
