package com.claim.platform.intake.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.claim.platform.intake.domain.model.IntakeStatus;
import com.claim.platform.intake.infrastructure.persistence.dataobject.person.AccidentPersonInfoDO;
import com.claim.platform.intake.infrastructure.persistence.dataobject.person.ApplicantInfoDO;
import com.claim.platform.intake.infrastructure.persistence.dataobject.person.MainInsuredInfoDO;
import com.claim.platform.intake.infrastructure.persistence.dataobject.person.PayeeInfoDO;
import lombok.Data;

/**
 * Claim Intake 所拥有的签收申请 MyBatis-Plus 数据对象，不向领域层泄露。
 * 四类人员信息以结构化 {@code PersonInfoDO} 承载，经 Jackson 类型处理器映射 JSON 列；
 * 列名由 MyBatis-Plus 下划线映射从字段名自动推导。
 *
 * @author wangguangwu
 */
@Data
@TableName(value = "t_claim_intake", autoResultMap = true)
public class ClaimIntakeDO {
    @TableId(type = IdType.AUTO) private Long id;
    @TableField("tenant_code") private String tenantCode;
    @TableField("sign_no") private String signNo;
    @TableField("external_apply_no") private String externalApplyNo;
    @TableField("applicant_name") private String applicantName;
    @TableField(value = "applicant_info", typeHandler = JacksonTypeHandler.class)
    private ApplicantInfoDO applicantInfo;
    @TableField(value = "accident_person_info", typeHandler = JacksonTypeHandler.class)
    private AccidentPersonInfoDO accidentPersonInfo;
    @TableField(value = "main_insured_info", typeHandler = JacksonTypeHandler.class)
    private MainInsuredInfoDO mainInsuredInfo;
    @TableField(value = "payee_info", typeHandler = JacksonTypeHandler.class)
    private PayeeInfoDO payeeInfo;
    @TableField("ext_json") private String extJson;
    @TableField("intake_status") private IntakeStatus status;
    @Version private Integer version;
    @TableField("is_deleted") private boolean deleted;
}
