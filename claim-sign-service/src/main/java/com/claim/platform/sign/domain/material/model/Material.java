package com.claim.platform.sign.domain.material.model;

/** Represents a material reference only; material source content never enters the aggregate or database.
 *
 * @author wangguangwu
 */
public record Material(String tenantCode, String signNo, String batchNo, String type, String name, String path,
                       String originalPath, boolean downloaded) {
    public Material {
        require(tenantCode, "tenantCode");
        require(signNo, "signNo");
        require(batchNo, "batchNo");
        require(type, "type");
        require(name, "name");
        require(path, "path");
    }

    private static void require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
