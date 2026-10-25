package com.claim.platform.common.api;

/**
 * Platform-wide errors that are independent of every bounded context.
 *
 * @author wangguangwu
 */
public enum PlatformErrorCode {
    REQUEST_VALIDATION_FAILED,
    UNAUTHORIZED,
    INTERNAL_ERROR
}
