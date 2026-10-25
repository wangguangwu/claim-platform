package com.claim.platform.sign.application.port;

/** Serializes a short business-critical section without becoming a source of business truth.
 *
 * @author wangguangwu
 */
public interface BusinessLock { <T> T execute(String key, LockedOperation<T> operation); interface LockedOperation<T> { T run(); } }
