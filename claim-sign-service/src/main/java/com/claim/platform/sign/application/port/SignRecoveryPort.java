package com.claim.platform.sign.application.port;

import com.claim.platform.sign.domain.sign.model.Sign;

/** Delegates recoverable external work while keeping compensation from mutating Sign state directly.
 *
 * @author wangguangwu
 */
public interface SignRecoveryPort {
    void resumePolicyCheck(Sign sign);

    boolean retryAutoUnderwriting(Sign sign);

    void resendRevoke(Sign sign);
}
