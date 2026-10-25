package com.claim.platform.sign.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/** Maps a tenant-scoped problem-record row while the domain retains its business invariants.
 *
 * @author wangguangwu
 */
@TableName("t_claim_sign_issue")
public class ClaimIssueDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tenantCode;
    private String signNo;
    private String issueNo;
    private String auditor;
    private String reason;
    private String extJson;
    private Integer isSyncTenant;
    private Integer isSupplement;
    private Integer isDeleted;
    public Long getId() { return id; }
    public void setId(Long value) { id = value; }
    public String getTenantCode() { return tenantCode; }
    public void setTenantCode(String value) { tenantCode = value; }
    public String getSignNo() { return signNo; }
    public void setSignNo(String value) { signNo = value; }
    public String getIssueNo() { return issueNo; }
    public void setIssueNo(String value) { issueNo = value; }
    public String getAuditor() { return auditor; }
    public void setAuditor(String value) { auditor = value; }
    public String getReason() { return reason; }
    public void setReason(String value) { reason = value; }
    public String getExtJson() { return extJson; }
    public void setExtJson(String value) { extJson = value; }
    public Integer getIsSyncTenant() { return isSyncTenant; }
    public void setIsSyncTenant(Integer value) { isSyncTenant = value; }
    public Integer getIsSupplement() { return isSupplement; }
    public void setIsSupplement(Integer value) { isSupplement = value; }
    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer value) { isDeleted = value; }
}
