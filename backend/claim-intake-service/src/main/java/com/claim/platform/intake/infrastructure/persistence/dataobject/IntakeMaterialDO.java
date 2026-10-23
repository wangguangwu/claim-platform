package com.claim.platform.intake.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * Claim Intake 所拥有的签收材料元数据 MyBatis-Plus 数据对象。
 *
 * @author wangguangwu
 */
@TableName("t_claim_intake_material")
public class IntakeMaterialDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("tenant_code")
    private String tenantCode;
    @TableField("sign_no")
    private String signNo;
    @TableField("batch_no")
    private String batchNo;
    @TableField("seq")
    private Integer sequence;
    private String type;
    private String name;
    private String path;
    @TableField("original_path")
    private String originalPath;
    @TableField("is_downloaded")
    private boolean downloaded;
    @TableField("is_deleted")
    private boolean deleted;

    public IntakeMaterialDO() {
    }

    public IntakeMaterialDO(String tenantCode, String signNo, String batchNo, Integer sequence, String type,
                            String name, String path, String originalPath) {
        this.tenantCode = tenantCode;
        this.signNo = signNo;
        this.batchNo = batchNo;
        this.sequence = sequence;
        this.type = type;
        this.name = name;
        this.path = path;
        this.originalPath = originalPath;
    }

    public Long getId() {
        return id;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public String getSignNo() {
        return signNo;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public Integer getSequence() {
        return sequence;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public String getOriginalPath() {
        return originalPath;
    }

    public boolean isDownloaded() {
        return downloaded;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public void setSignNo(String signNo) {
        this.signNo = signNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setOriginalPath(String originalPath) {
        this.originalPath = originalPath;
    }

    public void setDownloaded(boolean downloaded) {
        this.downloaded = downloaded;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
