package com.claim.platform.sign.application.result;

/** Returns the stable internal result of a created or replayed Sign fact.
 *
 * @author wangguangwu
 */
public record SignResult(String signNo, String status, boolean replayed) {
}
