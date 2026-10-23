package com.claim.platform.sign.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.claim.platform.sign.domain.model.SignStatus;
import com.claim.platform.sign.infrastructure.persistence.SignTable;
import com.claim.platform.sign.infrastructure.persistence.dataobject.person.AccidentPersonInfoDO;
import com.claim.platform.sign.infrastructure.persistence.dataobject.person.ApplicantInfoDO;
import com.claim.platform.sign.infrastructure.persistence.dataobject.person.MainInsuredInfoDO;
import com.claim.platform.sign.infrastructure.persistence.dataobject.person.PayeeInfoDO;
import lombok.Data;

/**
 * 理赔签收所拥有的签收申请 MyBatis-Plus 数据对象，不向领域层泄露。
 * 四类人员信息以结构化 {@code PersonInfoDO} 承载，经 Jackson 类型处理器映射 JSON 列。
 *
 * @author wangguangwu
 */
@Data
@TableName(value = SignTable.NAME, autoResultMap = true)
public class SignDO {
    @TableId(value = SignTable.ID, type = IdType.AUTO)
    private Long id;
    @TableField(SignTable.TENANT_CODE)
    private String tenantCode;
    @TableField(SignTable.SIGN_NO)
    private String signNo;
    @TableField(SignTable.EXTERNAL_APPLY_NO)
    private String externalApplyNo;
    @TableField(SignTable.APPLICANT_NAME)
    private String applicantName;
    @TableField(value = SignTable.APPLICANT_INFO, typeHandler = JacksonTypeHandler.class)
    private ApplicantInfoDO applicantInfo;
    @TableField(value = SignTable.ACCIDENT_PERSON_INFO, typeHandler = JacksonTypeHandler.class)
    private AccidentPersonInfoDO accidentPersonInfo;
    @TableField(value = SignTable.MAIN_INSURED_INFO, typeHandler = JacksonTypeHandler.class)
    private MainInsuredInfoDO mainInsuredInfo;
    @TableField(value = SignTable.PAYEE_INFO, typeHandler = JacksonTypeHandler.class)
    private PayeeInfoDO payeeInfo;
    @TableField(SignTable.EXT_JSON)
    private String extJson;
    @TableField(SignTable.INTAKE_STATUS)
    private SignStatus status;
    @Version
    @TableField(SignTable.VERSION)
    private Integer version;
    @TableField(SignTable.IS_DELETED)
    private boolean deleted;
}
