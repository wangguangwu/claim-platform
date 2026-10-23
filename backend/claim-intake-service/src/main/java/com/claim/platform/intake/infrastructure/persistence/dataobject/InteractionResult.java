package com.claim.platform.intake.infrastructure.persistence.dataobject;

/**
 * 交互审计处理结果，存储值与表结构注释保持一致。
 *
 * @author wangguangwu
 */
public enum InteractionResult {

    SUCCESS,
    FAILED;

    public String code() {
        return name();
    }
}
