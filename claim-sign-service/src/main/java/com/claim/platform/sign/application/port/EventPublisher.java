package com.claim.platform.sign.application.port;

/** Expresses a committed domain-event intent; concrete message routing belongs to later collaboration work.
 *
 * @author wangguangwu
 */
public interface EventPublisher {
    void publishSignAccepted(SignAcceptedEvent event);
}
