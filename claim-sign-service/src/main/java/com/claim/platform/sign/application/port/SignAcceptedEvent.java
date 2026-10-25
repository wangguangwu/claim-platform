package com.claim.platform.sign.application.port;

import java.time.Instant;

/** Carries the minimum versioned event-intent envelope required for a committed Sign acceptance.
 *
 * @author wangguangwu
 */
public record SignAcceptedEvent(String eventId, int eventVersion, String tenantCode, String aggregateType,
                                String signNo, Instant occurredAt, String traceId) {
}
