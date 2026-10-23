package com.claim.platform.sign.domain.model.person;

/**
 * 主被保险人：保单的主要被保人，附加保单号与被保人关系。
 * 作为 {@link Person} 封闭层级的一员，字段由 record 持有并覆盖根类型的抽象访问契约。
 *
 * @param name                姓名
 * @param certificateType     证件类型
 * @param certificateNo       证件号（PII）
 * @param mobile              手机号（PII）
 * @param gender              性别
 * @param birthDate           出生日期（ISO-8601）
 * @param policyNo            保单号
 * @param beneficiaryRelation 与被保人关系
 * @author wangguangwu
 */
public record MainInsured(String name, String certificateType, String certificateNo, String mobile, String gender,
                          String birthDate, String policyNo, String beneficiaryRelation) implements Person {
}
