package com.claim.platform.sign.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** MyBatis-Plus persistence representation of a Sign row; it contains no lifecycle behavior.
 *
 * @author wangguangwu
 */
@TableName("t_claim_sign")
public class SignDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tenantCode;
    private String signNo;
    private String externalApplyNo;
    private String applicantName;
    private String signStatus;
    private Integer protocolVersion;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer isDeleted;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTenantCode() { return tenantCode; }
    public void setTenantCode(String tenantCode) { this.tenantCode = tenantCode; }
    public String getSignNo() { return signNo; }
    public void setSignNo(String signNo) { this.signNo = signNo; }
    public String getExternalApplyNo() { return externalApplyNo; }
    public void setExternalApplyNo(String externalApplyNo) { this.externalApplyNo = externalApplyNo; }
    public String getApplicantName() { return applicantName; }
    public void setApplicantName(String applicantName) { this.applicantName = applicantName; }
    public String getSignStatus() { return signStatus; }
    public void setSignStatus(String signStatus) { this.signStatus = signStatus; }
    public Integer getProtocolVersion() { return protocolVersion; }
    public void setProtocolVersion(Integer protocolVersion) { this.protocolVersion = protocolVersion; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
}
