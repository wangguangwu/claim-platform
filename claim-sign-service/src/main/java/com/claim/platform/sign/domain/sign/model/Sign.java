package com.claim.platform.sign.domain.sign.model;

import com.claim.platform.sign.domain.sign.exception.InvalidSignStatusException;
import java.util.Objects;

/** Owns Sign lifecycle transitions and prevents terminal facts from being overwritten.
 *
 * @author wangguangwu
 */
public final class Sign {
    private final String tenantCode;
    private final SignNo signNo;
    private final String externalApplyNo;
    private final String applicantName;
    private final int protocolVersion;
    private SignStatus status;

    public Sign(String tenantCode, SignNo signNo, String externalApplyNo, String applicantName, int protocolVersion) {
        this(tenantCode, signNo, externalApplyNo, applicantName, protocolVersion, SignStatus.PENDING_ACCEPT);
    }

    public Sign(String tenantCode, SignNo signNo, String externalApplyNo, String applicantName, int protocolVersion,
                SignStatus status) {
        this.tenantCode = required(tenantCode, "tenantCode");
        this.signNo = Objects.requireNonNull(signNo, "signNo must not be null");
        this.externalApplyNo = required(externalApplyNo, "externalApplyNo");
        this.applicantName = applicantName;
        if (protocolVersion < 1) {
            throw new IllegalArgumentException("protocolVersion must be positive");
        }
        this.protocolVersion = protocolVersion;
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    /** Moves the aggregate only through its documented lifecycle graph. */
    public void transitionTo(SignStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidSignStatusException(signNo.value(), status.name(), target.name());
        }
        status = target;
    }

    /** Revokes any nonterminal Sign that has not already reached a terminal outcome. */
    public void revoke() {
        transitionTo(SignStatus.CLAIM_REVOKED);
    }

    public String tenantCode() { return tenantCode; }
    public SignNo signNo() { return signNo; }
    public String externalApplyNo() { return externalApplyNo; }
    public String applicantName() { return applicantName; }
    public int protocolVersion() { return protocolVersion; }
    public SignStatus status() { return status; }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
