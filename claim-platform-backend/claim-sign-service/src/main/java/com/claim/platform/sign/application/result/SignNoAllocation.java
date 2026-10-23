package com.claim.platform.sign.application.result;

/**
 * 一次签收号分配结果，包含编号和实际发号来源。
 *
 * @param signNo 平台签收号
 * @param source 发号来源
 * @author wangguangwu
 */
public record SignNoAllocation(String signNo, SignNoSource source) {
}
