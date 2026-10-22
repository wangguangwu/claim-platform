package com.claim.platform.common.web;

/**
 * 定义 Gateway 暴露接口使用的稳定响应信封，避免调用方依赖各服务的异常实现细节。
 * 领域错误码由各自的限界上下文定义，当前类型只承载无领域含义的响应结构。
 *
 * @param <T> 成功响应携带的数据类型
 * @author wangguangwu
 */
public record ApiResult<T>(boolean success, String code, String message, String traceId, T data) {

    /**
     * 创建成功响应。
     *
     * @param data 返回数据，可以为 {@code null}
     * @param traceId 用于调用方与日志关联的追踪标识
     * @param <T> 返回数据类型
     * @return 成功响应信封
     */
    public static <T> ApiResult<T> success(T data, String traceId) {
        return new ApiResult<>(true, "OK", "操作成功", traceId, data);
    }

    /**
     * 创建不携带业务数据的失败响应。
     *
     * @param code 稳定的机器可读错误码
     * @param message 可安全展示给调用方的错误说明
     * @param traceId 用于调用方与日志关联的追踪标识
     * @return 失败响应信封
     */
    public static ApiResult<Void> failure(String code, String message, String traceId) {
        return new ApiResult<>(false, code, message, traceId, null);
    }
}
