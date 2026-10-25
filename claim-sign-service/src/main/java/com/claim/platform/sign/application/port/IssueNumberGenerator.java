package com.claim.platform.sign.application.port;

/**
 * Allocates a problem-record identifier unique only inside one tenant Sign scope.
 *
 * @author wangguangwu
 */
public interface IssueNumberGenerator {
    String next(String tenantCode, String signNo);
}
