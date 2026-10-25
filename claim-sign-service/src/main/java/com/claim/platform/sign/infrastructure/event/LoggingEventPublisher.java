package com.claim.platform.sign.infrastructure.event;

import com.claim.platform.sign.application.port.EventPublisher;
import com.claim.platform.sign.application.port.SignAcceptedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Logs committed event intent until the collaboration Feature supplies a RocketMQ implementation.
 *
 * @author wangguangwu
 */
@Component
public class LoggingEventPublisher implements EventPublisher {
    private static final Logger LOG = LoggerFactory.getLogger(LoggingEventPublisher.class);
    @Override public void publishSignAccepted(SignAcceptedEvent event) {
        LOG.info("eventIntent=SIGN_ACCEPT eventId={} eventVersion={} tenantCode={} aggregateType={} signNo={} traceId={}",
                event.eventId(), event.eventVersion(), event.tenantCode(), event.aggregateType(), event.signNo(),
                event.traceId());
    }
}
