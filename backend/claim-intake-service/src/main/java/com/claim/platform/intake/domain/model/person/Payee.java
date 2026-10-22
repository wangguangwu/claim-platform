package com.claim.platform.intake.domain.model.person;

/**
 * 领款人：接收赔款的当事方，附加开户行、银行账号（PII）与出险人关系。
 * 作为 {@link Person} 封闭层级的一员，字段由 record 持有并覆盖根类型的抽象访问契约。
 *
 * @author wangguangwu
 * @param name 姓名
 * @param certificateType 证件类型
 * @param certificateNo 证件号（PII）
 * @param mobile 手机号（PII）
 * @param gender 性别
 * @param birthDate 出生日期（ISO-8601）
 * @param bankName 开户行
 * @param bankAccount 银行账号（PII，日志与响应中不得原文输出）
 * @param payeeRelation 与出险人关系
 */
public record Payee(String name, String certificateType, String certificateNo, String mobile, String gender,
                   String birthDate, String bankName, String bankAccount, String payeeRelation) implements Person {
}
