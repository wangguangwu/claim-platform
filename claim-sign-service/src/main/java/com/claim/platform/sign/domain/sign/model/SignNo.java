package com.claim.platform.sign.domain.sign.model;

/** Value object for the globally unique platform Sign identifier.
 *
 * @author wangguangwu
 */
public record SignNo(String value) {
    public SignNo {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("signNo must not be blank");
        }
    }
}
