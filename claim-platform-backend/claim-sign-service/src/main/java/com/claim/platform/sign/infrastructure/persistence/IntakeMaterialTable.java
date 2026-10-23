package com.claim.platform.sign.infrastructure.persistence;

/**
 * 定义 Claim Intake 材料表的表名和列名，供 MyBatis-Plus 数据对象共同使用。
 *
 * @author wangguangwu
 */
public final class IntakeMaterialTable {

    public static final String NAME = "t_claim_intake_material";
    public static final String ID = "id";
    public static final String TENANT_CODE = "tenant_code";
    public static final String SIGN_NO = "sign_no";
    public static final String BATCH_NO = "batch_no";
    public static final String SEQUENCE = "seq";
    public static final String TYPE = "type";
    public static final String NAME_FIELD = "name";
    public static final String PATH = "path";
    public static final String ORIGINAL_PATH = "original_path";
    public static final String IS_DOWNLOADED = "is_downloaded";
    public static final String IS_DELETED = "is_deleted";

    private IntakeMaterialTable() {
    }
}
