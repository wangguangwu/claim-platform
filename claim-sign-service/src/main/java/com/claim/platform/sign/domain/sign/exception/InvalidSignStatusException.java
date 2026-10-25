package com.claim.platform.sign.domain.sign.exception;

/** Raised when a Sign state transition would violate the immutable lifecycle graph.
 *
 * @author wangguangwu
 */
public class InvalidSignStatusException extends RuntimeException {
    public InvalidSignStatusException(String signNo, String currentStatus, String targetStatus) {
        super("Sign status transition is not allowed: signNo=%s, from=%s, to=%s"
                .formatted(signNo, currentStatus, targetStatus));
    }
}
