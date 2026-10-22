package com.claim.platform.intake.domain.model.person;

/**
 * 申请人：发起签收申请的当事方，附加与投保人/被保人关系。
 * 作为 {@link Person} 封闭层级的一员，字段由 record 持有并覆盖根类型的抽象访问契约。
 *
 * @author wangguangwu
 * @param name 姓名
 * @param certificateType 证件类型
 * @param certificateNo 证件号（PII）
 * @param mobile 手机号（PII）
 * @param gender 性别
 * @param birthDate 出生日期（ISO-8601）
 * @param relationshipToInsured 与投保人/被保人关系
 */
public record Applicant(String name, String certificateType, String certificateNo, String mobile, String gender,
                       String birthDate, String relationshipToInsured) implements Person {
}
