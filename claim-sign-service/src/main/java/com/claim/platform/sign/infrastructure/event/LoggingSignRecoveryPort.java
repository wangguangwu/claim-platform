package com.claim.platform.sign.infrastructure.event;

import com.claim.platform.sign.application.port.SignRecoveryPort;
import com.claim.platform.sign.domain.sign.model.Sign;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Emits structured recovery intent until the collaboration feature installs external integrations.
 *
 * @author wangguangwu
 */
@Component
public class LoggingSignRecoveryPort implements SignRecoveryPort {
    private static final Logger LOG = LoggerFactory.getLogger(LoggingSignRecoveryPort.class);
    @Override public void resumePolicyCheck(Sign sign) { log(sign, "RESUME_POLICY_CHECK"); }
    @Override public boolean retryAutoUnderwriting(Sign sign) { log(sign, "AUTO_UNDERWRITING_MANUAL_REQUIRED"); return false; }
    @Override public void resendRevoke(Sign sign) { log(sign, "RESEND_CLAIM_REVOKE"); }
    private void log(Sign sign, String action) {
        LOG.warn("recoveryAction={} tenantCode={} signNo={} signStatus={}", action, sign.tenantCode(),
                sign.signNo().value(), sign.status());
    }
}
