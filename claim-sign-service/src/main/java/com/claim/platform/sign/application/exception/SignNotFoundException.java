package com.claim.platform.sign.application.exception;

/** Raised when a tenant-scoped Sign fact cannot be found.
 *
 * @author wangguangwu
 */
public class SignNotFoundException extends RuntimeException {
    public SignNotFoundException(String signNo) {
        super("Sign was not found: signNo=" + signNo);
    }
}
