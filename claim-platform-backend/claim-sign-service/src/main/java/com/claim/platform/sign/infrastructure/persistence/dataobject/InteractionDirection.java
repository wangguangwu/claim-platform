package com.claim.platform.sign.infrastructure.persistence.dataobject;

/**
 * 交互审计方向，存储值与表结构注释保持一致。
 *
 * @author wangguangwu
 */
public enum InteractionDirection {

    IN,
    OUT;

    public String code() {
        return name();
    }
}
