package com.claim.platform.intake.domain.model.person;

/**
 * 出险人：发生保险事故的当事方，附加出险日期与出险类型。
 * 作为 {@link Person} 封闭层级的一员，字段由 record 持有并覆盖根类型的抽象访问契约。
 *
 * @param name            姓名
 * @param certificateType 证件类型
 * @param certificateNo   证件号（PII）
 * @param mobile          手机号（PII）
 * @param gender          性别
 * @param birthDate       出生日期（ISO-8601）
 * @param accidentDate    出险日期
 * @param accidentType    出险类型
 * @author wangguangwu
 */
public record AccidentPerson(String name, String certificateType, String certificateNo, String mobile, String gender,
                             String birthDate, String accidentDate, String accidentType) implements Person {
}
