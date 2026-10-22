package com.claim.platform.application.domain.event;

import java.time.Instant;

/**
 * 表示本地事务中追加的、尚待异步发布的版本化领域事件意图。
 *
 * @author wangguangwu
 */
public record OutboxEvent(String eventId, String eventType, String eventVersion, String tenantCode,
                          String aggregateType, String aggregateId, String payload, String traceId,
                          Instant occurredAt) {
}
