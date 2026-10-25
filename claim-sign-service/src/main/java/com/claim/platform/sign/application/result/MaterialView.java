package com.claim.platform.sign.application.result;

/** Is the internal projection of material metadata and never contains material content.
 *
 * @author wangguangwu
 */
public record MaterialView(String batchNo, String type, String name, String path, String originalPath,
                           boolean downloaded) {
}
