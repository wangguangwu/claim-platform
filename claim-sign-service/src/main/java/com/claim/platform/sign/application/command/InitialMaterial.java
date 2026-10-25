package com.claim.platform.sign.application.command;

/** Describes initial material metadata before the created SignNo can bind it to a Sign.
 *
 * @author wangguangwu
 */
public record InitialMaterial(String batchNo, String type, String name, String path, String originalPath) {
    public InitialMaterial {
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
