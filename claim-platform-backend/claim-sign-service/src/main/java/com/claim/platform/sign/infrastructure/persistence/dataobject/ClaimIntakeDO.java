package com.claim.platform.sign.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.claim.platform.sign.domain.model.IntakeStatus;
import com.claim.platform.sign.infrastructure.persistence.ClaimIntakeTable;
import com.claim.platform.sign.infrastructure.persistence.dataobject.person.AccidentPersonInfoDO;
import com.claim.platform.sign.infrastructure.persistence.dataobject.person.ApplicantInfoDO;
import com.claim.platform.sign.infrastructure.persistence.dataobject.person.MainInsuredInfoDO;
import com.claim.platform.sign.infrastructure.persistence.dataobject.person.PayeeInfoDO;
import lombok.Data;

/**
 * Claim Intake 所拥有的签收申请 MyBatis-Plus 数据对象，不向领域层泄露。
 * 四类人员信息以结构化 {@code PersonInfoDO} 承载，经 Jackson 类型处理器映射 JSON 列。
 *
 * @author wangguangwu
 */
@Data
@TableName(value = ClaimIntakeTable.NAME, autoResultMap = true)
public class ClaimIntakeDO {
    @TableId(value = ClaimIntakeTable.ID, type = IdType.AUTO)
    private Long id;
    @TableField(ClaimIntakeTable.TENANT_CODE)
    private String tenantCode;
    @TableField(ClaimIntakeTable.SIGN_NO)
    private String signNo;
    @TableField(ClaimIntakeTable.EXTERNAL_APPLY_NO)
    private String externalApplyNo;
    @TableField(ClaimIntakeTable.APPLICANT_NAME)
    private String applicantName;
    @TableField(value = ClaimIntakeTable.APPLICANT_INFO, typeHandler = JacksonTypeHandler.class)
    private ApplicantInfoDO applicantInfo;
    @TableField(value = ClaimIntakeTable.ACCIDENT_PERSON_INFO, typeHandler = JacksonTypeHandler.class)
    private AccidentPersonInfoDO accidentPersonInfo;
    @TableField(value = ClaimIntakeTable.MAIN_INSURED_INFO, typeHandler = JacksonTypeHandler.class)
    private MainInsuredInfoDO mainInsuredInfo;
    @TableField(value = ClaimIntakeTable.PAYEE_INFO, typeHandler = JacksonTypeHandler.class)
    private PayeeInfoDO payeeInfo;
    @TableField(ClaimIntakeTable.EXT_JSON)
    private String extJson;
    @TableField(ClaimIntakeTable.INTAKE_STATUS)
    private IntakeStatus status;
    @Version
    @TableField(ClaimIntakeTable.VERSION)
    private Integer version;
    @TableField(ClaimIntakeTable.IS_DELETED)
    private boolean deleted;
}
