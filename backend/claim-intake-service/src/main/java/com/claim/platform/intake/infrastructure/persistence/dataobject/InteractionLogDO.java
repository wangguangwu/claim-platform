package com.claim.platform.intake.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 保存不含敏感请求内容的签收交互审计日志。
 *
 * @author wangguangwu
 */
@TableName("t_claim_intake_interaction_log")
public class InteractionLogDO {
    @TableId(type = IdType.AUTO) private Long id;
    @TableField("tenant_code") private String tenantCode;
    @TableField("sign_no") private String signNo;
    @TableField("interaction_type") private String interactionType;
    private String direction;
    @TableField("request_id") private String requestId;
    @TableField("trace_id") private String traceId;
    private String result;

    public InteractionLogDO() {
    }

    public InteractionLogDO(String tenantCode, String signNo, String interactionType, String requestId, String traceId) {
        this.tenantCode = tenantCode;
        this.signNo = signNo;
        this.interactionType = interactionType;
        this.direction = "IN";
        this.requestId = requestId;
        this.traceId = traceId;
        this.result = "SUCCESS";
    }
}
