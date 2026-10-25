package com.claim.platform.sign.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/** Persists a material location without retaining material content. @author wangguangwu */
@TableName("t_claim_sign_material")
public class MaterialDO {
    @TableId(type = IdType.AUTO) private Long id;
    private String tenantCode, signNo, batchNo, type, name, path, originalPath;
    private Integer isDownloaded, isDeleted;
    public Long getId(){return id;} public void setId(Long v){id=v;} public String getTenantCode(){return tenantCode;} public void setTenantCode(String v){tenantCode=v;} public String getSignNo(){return signNo;} public void setSignNo(String v){signNo=v;} public String getBatchNo(){return batchNo;} public void setBatchNo(String v){batchNo=v;} public String getType(){return type;} public void setType(String v){type=v;} public String getName(){return name;} public void setName(String v){name=v;} public String getPath(){return path;} public void setPath(String v){path=v;} public String getOriginalPath(){return originalPath;} public void setOriginalPath(String v){originalPath=v;} public Integer getIsDownloaded(){return isDownloaded;} public void setIsDownloaded(Integer v){isDownloaded=v;} public Integer getIsDeleted(){return isDeleted;} public void setIsDeleted(Integer v){isDeleted=v;}
}
