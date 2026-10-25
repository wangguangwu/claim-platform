package com.claim.platform.sign.domain.sign.model;

import java.util.EnumSet;
import java.util.Set;

/** Defines the only lifecycle state field of a Sign aggregate and its legal transitions.
 *
 * @author wangguangwu
 */
public enum SignStatus {
    PENDING_ACCEPT,
    ACCEPT_SUCCESS,
    ACCEPT_FAILED,
    AUTO_UNDERWRITING,
    WAIT_AUDIT_ACCEPT,
    SIGN_COMPLETED,
    ISSUE_CASE,
    CLAIM_REVOKED;

    private static final Set<SignStatus> TERMINAL = EnumSet.of(ACCEPT_FAILED, SIGN_COMPLETED, CLAIM_REVOKED);

    /** Returns whether this state may move to the requested target state. */
    public boolean canTransitionTo(SignStatus target) {
        return switch (this) {
            case PENDING_ACCEPT -> target == ACCEPT_SUCCESS || target == ACCEPT_FAILED || target == CLAIM_REVOKED;
            case ACCEPT_SUCCESS -> target == AUTO_UNDERWRITING || target == WAIT_AUDIT_ACCEPT || target == CLAIM_REVOKED;
            case AUTO_UNDERWRITING -> target == WAIT_AUDIT_ACCEPT || target == CLAIM_REVOKED;
            case WAIT_AUDIT_ACCEPT -> target == SIGN_COMPLETED || target == ISSUE_CASE || target == CLAIM_REVOKED;
            case ISSUE_CASE -> target == WAIT_AUDIT_ACCEPT || target == CLAIM_REVOKED;
            case ACCEPT_FAILED, SIGN_COMPLETED, CLAIM_REVOKED -> false;
        };
    }

    /** Returns whether automatic state changes must stop permanently. */
    public boolean isTerminal() {
        return TERMINAL.contains(this);
    }
}
