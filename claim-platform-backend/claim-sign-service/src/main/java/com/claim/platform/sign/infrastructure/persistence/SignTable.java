package com.claim.platform.sign.infrastructure.persistence;

/**
 * 定义理赔签收申请表的表名、列名和唯一索引名，供数据对象、查询和启动校验共同使用。
 *
 * @author wangguangwu
 */
public final class SignTable {

    public static final String NAME = "t_claim_intake";
    public static final String ID = "id";
    public static final String TENANT_CODE = "tenant_code";
    public static final String SIGN_NO = "sign_no";
    public static final String EXTERNAL_APPLY_NO = "external_apply_no";
    public static final String APPLICANT_NAME = "applicant_name";
    public static final String APPLICANT_INFO = "applicant_info";
    public static final String ACCIDENT_PERSON_INFO = "accident_person_info";
    public static final String MAIN_INSURED_INFO = "main_insured_info";
    public static final String PAYEE_INFO = "payee_info";
    public static final String EXT_JSON = "ext_json";
    public static final String INTAKE_STATUS = "intake_status";
    public static final String VERSION = "version";
    public static final String IS_DELETED = "is_deleted";
    public static final String INDEX_UK_SIGN_NO = "uk_sign_no";

    private SignTable() {
    }
}
