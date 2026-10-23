package com.claim.platform.sign.infrastructure.persistence;

/**
 * 定义 Claim Intake 交互审计表的表名和列名，供 MyBatis-Plus 数据对象共同使用。
 *
 * @author wangguangwu
 */
public final class InteractionLogTable {

    public static final String NAME = "t_claim_intake_interaction_log";
    public static final String ID = "id";
    public static final String TENANT_CODE = "tenant_code";
    public static final String SIGN_NO = "sign_no";
    public static final String INTERACTION_TYPE = "interaction_type";
    public static final String DIRECTION = "direction";
    public static final String REQUEST_ID = "request_id";
    public static final String TRACE_ID = "trace_id";
    public static final String RESULT = "result";

    private InteractionLogTable() {
    }
}
