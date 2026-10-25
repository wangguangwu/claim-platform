package com.claim.platform.sign.application.exception;

/** Signals a retryable concurrent Sign creation outcome whose winning fact is not yet readable.
 *
 * @author wangguangwu
 */
public class ConcurrentSignModificationException extends RuntimeException {
    public ConcurrentSignModificationException(String tenantCode, String externalApplyNo, Throwable cause) {
        super("Concurrent Sign creation requires retry: tenantCode=%s, externalApplyNo=%s"
                .formatted(tenantCode, externalApplyNo), cause);
    }
}
