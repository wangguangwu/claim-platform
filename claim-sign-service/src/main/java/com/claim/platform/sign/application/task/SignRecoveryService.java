package com.claim.platform.sign.application.task;

import com.claim.platform.common.trace.TraceContext;
import com.claim.platform.sign.application.port.EventPublisher;
import com.claim.platform.sign.application.port.SignAcceptedEvent;
import com.claim.platform.sign.application.port.SignRecoveryPort;
import com.claim.platform.sign.domain.sign.model.Sign;
import com.claim.platform.sign.domain.sign.model.SignStatus;
import com.claim.platform.sign.domain.sign.repository.SignRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Selects the documented state-specific compensation action without directly changing Sign status.
 *
 * @author wangguangwu
 */
@Service
public class SignRecoveryService {
    private static final Logger LOG = LoggerFactory.getLogger(SignRecoveryService.class);
    private final SignRepository repository;
    private final EventPublisher eventPublisher;
    private final SignRecoveryPort recoveryPort;

    public SignRecoveryService(SignRepository repository, EventPublisher eventPublisher, SignRecoveryPort recoveryPort) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.recoveryPort = recoveryPort;
    }

    /** Scans stale Sign facts and delegates each one to the state-specific recovery strategy. */
    public void recoverStalledSigns() {
        repository.findStalledBefore(Instant.now().minus(5, ChronoUnit.MINUTES), 200).forEach(this::recover);
    }

    /** Recovers one Sign intent and never changes its persisted lifecycle state directly. */
    public void recover(Sign sign) {
        switch (sign.status()) {
            case PENDING_ACCEPT -> eventPublisher.publishSignAccepted(new SignAcceptedEvent(UUID.randomUUID().toString(), 1,
                    sign.tenantCode(), "Sign", sign.signNo().value(), Instant.now(), TraceContext.currentOrCreate()));
            case ACCEPT_SUCCESS -> recoveryPort.resumePolicyCheck(sign);
            case AUTO_UNDERWRITING -> {
                if (!recoveryPort.retryAutoUnderwriting(sign)) { signal(sign, "MANUAL_PROCESSING", "AUTO_UNDERWRITING_NOT_RETRYABLE"); }
            }
            case WAIT_AUDIT_ACCEPT -> signal(sign, "TIMEOUT", "AUDIT_WAIT_NO_RESUBMIT");
            case ISSUE_CASE -> signal(sign, "WAITING", "ISSUE_REPLY_REQUIRED");
            case ACCEPT_FAILED -> signal(sign, "TERMINAL", "ACCEPT_FAILED_NO_RECOVERY");
            case SIGN_COMPLETED -> { }
            case CLAIM_REVOKED -> recoveryPort.resendRevoke(sign);
        }
    }

    private void signal(Sign sign, String failureCategory, String recoveryAction) {
        LOG.warn("recoverySignal=true traceId={} tenantCode={} signNo={} signStatus={} failureCategory={} recoveryAction={}",
                TraceContext.currentOrCreate(), sign.tenantCode(), sign.signNo().value(), sign.status(), failureCategory,
                recoveryAction);
    }
}
