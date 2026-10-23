package com.claim.platform.sign.domain.exception;

/**
 * 表示申请状态在本次写入期间已被并发修改，调用方应重新查询后以新幂等键决定是否重试。
 *
 * @author wangguangwu
 */
public class ConcurrentSignModificationException extends RuntimeException {
    public ConcurrentSignModificationException() {
        super("签收申请已被并发修改");
    }
}
