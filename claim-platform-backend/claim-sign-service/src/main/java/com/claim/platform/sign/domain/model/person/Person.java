package com.claim.platform.sign.domain.model.person;

/**
 * 理赔场景中四类核心人员的标准领域模型根类型。
 * 以封闭接口层级约束仅存在申请人、出险人、主被保险人与领款人四种角色；
 * record 子类型通过实现本接口覆盖通用身份访问契约（record 不能继承类，故以接口为根）。
 * 领域层不依赖持久化、JSON 或 MapStruct。
 *
 * @author wangguangwu
 */
public sealed interface Person permits Applicant, AccidentPerson, MainInsured, Payee {

    /**
     * 姓名。
     *
     * @return 人员姓名
     */
    String name();

    /**
     * 证件类型，如 ID_CARD。
     *
     * @return 证件类型编码
     */
    String certificateType();

    /**
     * 证件号码，属个人敏感信息，日志与响应中不得原文输出。
     *
     * @return 证件号码
     */
    String certificateNo();

    /**
     * 手机号，属个人敏感信息，日志与响应中不得原文输出。
     *
     * @return 手机号码
     */
    String mobile();

    /**
     * 性别。
     *
     * @return 性别编码
     */
    String gender();

    /**
     * 出生日期，ISO-8601 格式。
     *
     * @return 出生日期
     */
    String birthDate();
}
