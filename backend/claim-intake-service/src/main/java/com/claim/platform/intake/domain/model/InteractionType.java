package com.claim.platform.intake.domain.model;

/**
 * 平台签收交互类型；持久化时使用枚举名称，不保存展示文案。
 *
 * @author wangguangwu
 */
public enum InteractionType {

    SIGN_RECEIVE,
    IMAGE_UPLOAD;

    public String code() {
        return name();
    }
}
