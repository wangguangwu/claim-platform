package com.claim.platform.sign.domain.sign.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.claim.platform.sign.domain.sign.exception.InvalidSignStatusException;
import org.junit.jupiter.api.Test;

/** Verifies Sign lifecycle and terminal-state invariants. @author wangguangwu */
class SignTest {
    @Test
    void shouldFollowEightStateLifecycleToCompletion() {
        Sign sign = new Sign("tenant-a", new SignNo("S202610250001"), "external-1", "applicant", 1);

        sign.transitionTo(SignStatus.ACCEPT_SUCCESS);
        sign.transitionTo(SignStatus.AUTO_UNDERWRITING);
        sign.transitionTo(SignStatus.WAIT_AUDIT_ACCEPT);
        sign.transitionTo(SignStatus.ISSUE_CASE);
        sign.transitionTo(SignStatus.WAIT_AUDIT_ACCEPT);
        sign.transitionTo(SignStatus.SIGN_COMPLETED);

        assertThat(sign.status()).isEqualTo(SignStatus.SIGN_COMPLETED);
    }

    @Test
    void shouldRejectTransitionFromTerminalState() {
        Sign sign = new Sign("tenant-a", new SignNo("S202610250002"), "external-2", null, 1);
        sign.transitionTo(SignStatus.ACCEPT_FAILED);

        assertThatThrownBy(() -> sign.transitionTo(SignStatus.ACCEPT_SUCCESS))
                .isInstanceOf(InvalidSignStatusException.class);
    }

    @Test
    void shouldAllowRevokeOnlyBeforeTerminalOutcome() {
        Sign sign = new Sign("tenant-a", new SignNo("S202610250003"), "external-3", null, 1);
        sign.revoke();

        assertThat(sign.status()).isEqualTo(SignStatus.CLAIM_REVOKED);
        assertThatThrownBy(sign::revoke).isInstanceOf(InvalidSignStatusException.class);
    }
}
