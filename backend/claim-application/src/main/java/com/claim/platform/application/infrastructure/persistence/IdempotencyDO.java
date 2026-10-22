package com.claim.platform.application.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 保存签收命令幂等占位和首次结果，避免重复产生业务副作用。
 *
 * @author wangguangwu
 */
@TableName("t_sign_idempotency")
public class IdempotencyDO {
    @TableId(type = IdType.AUTO) private Long id;
    @TableField("tenant_code") private String tenantCode;
    private String operation;
    @TableField("idempotency_key") private String idempotencyKey;
    @TableField("request_hash") private String requestHash;
    @TableField("processing_status") private String processingStatus;
    @TableField("result_sign_no") private String resultSignNo;
    @TableField("completed_at") private LocalDateTime completedAt;

    public IdempotencyDO() {
    }

    public String getRequestHash() { return requestHash; }
    public String getProcessingStatus() { return processingStatus; }
    public String getResultSignNo() { return resultSignNo; }
}
