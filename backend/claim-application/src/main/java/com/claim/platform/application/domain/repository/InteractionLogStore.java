package com.claim.platform.application.domain.repository;

/**
 * 定义签收交互审计日志端口，日志不保存请求体或材料原文。
 *
 * @author wangguangwu
 */
public interface InteractionLogStore {

    /**
     * 记录本地命令成功的审计事实，并关联 traceId 与请求幂等键。
     */
    void recordSuccess(String tenantCode, String signNo, String interactionType, String requestId, String traceId);
}
