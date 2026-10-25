package com.claim.platform.sign.infrastructure.job;

import com.claim.platform.sign.application.task.SignRecoveryService;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.stereotype.Component;

/** Exposes the state-based Sign compensation scan to the configured XXL-Job executor.
 *
 * @author wangguangwu
 */
@Component
public class SignRecoveryJobHandler {
    private final SignRecoveryService recoveryService;
    public SignRecoveryJobHandler(SignRecoveryService recoveryService) { this.recoveryService = recoveryService; }
    /** Runs the bounded stale-Sign scan selected by the local XXL-Job schedule. */
    @XxlJob("signRecoveryJobHandler")
    public void recover() { recoveryService.recoverStalledSigns(); }
}
