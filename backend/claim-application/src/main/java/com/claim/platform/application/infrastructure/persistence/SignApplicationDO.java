package com.claim.platform.application.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.claim.platform.application.domain.model.SignApplicationStatus;

/**
 * Application Intake 所拥有的签收申请 MyBatis-Plus 数据对象，不向领域层泄露。
 *
 * @author wangguangwu
 */
@TableName("t_sign_application")
public class SignApplicationDO {
    @TableId(type = IdType.AUTO) private Long id;
    @TableField("tenant_code") private String tenantCode;
    @TableField("sign_no") private String signNo;
    @TableField("external_apply_no") private String externalApplyNo;
    @TableField("applicant_name") private String applicantName;
    @TableField("applicant_info") private String applicantInfo;
    @TableField("accident_person_info") private String accidentPersonInfo;
    @TableField("main_insured_info") private String mainInsuredInfo;
    @TableField("payee_info") private String payeeInfo;
    @TableField("ext_json") private String extJson;
    @TableField("application_status") private SignApplicationStatus status;
    @Version private Integer version;
    @TableField("is_deleted") private boolean deleted;

    public SignApplicationDO() {
    }

    public SignApplicationDO(String tenantCode, String signNo, String externalApplyNo, String applicantName,
                          String applicantInfo, String accidentPersonInfo, String mainInsuredInfo,
                          String payeeInfo, String extJson, SignApplicationStatus status) {
        this.tenantCode = tenantCode;
        this.signNo = signNo;
        this.externalApplyNo = externalApplyNo;
        this.applicantName = applicantName;
        this.applicantInfo = applicantInfo;
        this.accidentPersonInfo = accidentPersonInfo;
        this.mainInsuredInfo = mainInsuredInfo;
        this.payeeInfo = payeeInfo;
        this.extJson = extJson;
        this.status = status;
    }

    public Long getId() { return id; }
    public String getTenantCode() { return tenantCode; }
    public String getSignNo() { return signNo; }
    public String getExternalApplyNo() { return externalApplyNo; }
    public String getApplicantName() { return applicantName; }
    public String getApplicantInfo() { return applicantInfo; }
    public String getAccidentPersonInfo() { return accidentPersonInfo; }
    public String getMainInsuredInfo() { return mainInsuredInfo; }
    public String getPayeeInfo() { return payeeInfo; }
    public String getExtJson() { return extJson; }
    public SignApplicationStatus getStatus() { return status; }
    public Integer getVersion() { return version; }
    public boolean isDeleted() { return deleted; }
    public void setId(Long id) { this.id = id; }
    public void setTenantCode(String tenantCode) { this.tenantCode = tenantCode; }
    public void setSignNo(String signNo) { this.signNo = signNo; }
    public void setExternalApplyNo(String externalApplyNo) { this.externalApplyNo = externalApplyNo; }
    public void setApplicantName(String applicantName) { this.applicantName = applicantName; }
    public void setApplicantInfo(String applicantInfo) { this.applicantInfo = applicantInfo; }
    public void setAccidentPersonInfo(String accidentPersonInfo) { this.accidentPersonInfo = accidentPersonInfo; }
    public void setMainInsuredInfo(String mainInsuredInfo) { this.mainInsuredInfo = mainInsuredInfo; }
    public void setPayeeInfo(String payeeInfo) { this.payeeInfo = payeeInfo; }
    public void setExtJson(String extJson) { this.extJson = extJson; }
    public void setStatus(SignApplicationStatus status) { this.status = status; }
    public void setVersion(Integer version) { this.version = version; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
}
