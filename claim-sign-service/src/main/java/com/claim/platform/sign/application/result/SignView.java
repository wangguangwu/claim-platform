package com.claim.platform.sign.application.result;

/** Is the tenant-scoped internal projection of a Sign fact for upper application layers.
 *
 * @author wangguangwu
 */
public record SignView(String signNo, String externalApplyNo, String applicantName, String status,
                       int protocolVersion) {
}
