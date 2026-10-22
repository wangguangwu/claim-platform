package com.claim.platform.intake.infrastructure.persistence.person;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 人员信息持久化对象的抽象基类，承载四类角色通用的身份字段。
 * 由 MyBatis-Plus {@code JacksonTypeHandler} 将 JSON 列反序列化为具体子类；
 * 不向领域层泄露，字段命名与 {@code Person} 子类型一致以便 MapStruct 自动映射。
 *
 * @author wangguangwu
 */
@Getter
@Setter
@NoArgsConstructor
public abstract class PersonInfoDO {

    private String name;
    private String certificateType;
    private String certificateNo;
    private String mobile;
    private String gender;
    private String birthDate;
}
