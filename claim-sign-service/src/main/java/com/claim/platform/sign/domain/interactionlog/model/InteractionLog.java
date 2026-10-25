package com.claim.platform.sign.domain.interactionlog.model;

import java.time.Instant;

/** Stores a sanitized cross-system interaction audit entry without request or response content.
 *
 * @author wangguangwu
 */
public record InteractionLog(String tenantCode, String signNo, String interactionType, String direction,
                             String sourceSystem, String targetSystem, String requestId, String traceId,
                             String result, String errorCode, String message, Long costMs, Instant occurredAt) {
    public InteractionLog {
        if (tenantCode == null || tenantCode.isBlank() || signNo == null || signNo.isBlank()
                || interactionType == null || interactionType.isBlank() || direction == null || direction.isBlank()
                || result == null || result.isBlank() || occurredAt == null) {
            throw new IllegalArgumentException("interaction log required fields must not be blank");
        }
    }
}
