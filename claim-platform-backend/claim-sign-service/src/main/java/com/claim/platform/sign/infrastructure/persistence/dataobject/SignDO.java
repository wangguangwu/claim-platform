package com.claim.platform.sign.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.claim.platform.sign.domain.model.SignStatus;
import com.claim.platform.sign.infrastructure.persistence.SignTable;
import lombok.Data;

/**
 * 理赔签收所拥有的签收申请 MyBatis-Plus 数据对象，不向领域层泄露。
 * 人员明细不落在结构化列上，只写 {@code applicant_name} 与 {@code ext_json}。
 *
 * @author wangguangwu
 */
@Data
@TableName(SignTable.NAME)
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
