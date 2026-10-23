package com.claim.platform.sign.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.claim.platform.sign.infrastructure.persistence.InteractionLogTable;

/**
 * 保存不含敏感请求内容的签收交互审计日志。
 *
 * @author wangguangwu
 */
@TableName(InteractionLogTable.NAME)
public class InteractionLogDO {
    @TableId(value = InteractionLogTable.ID, type = IdType.AUTO)
    private Long id;
    @TableField(InteractionLogTable.TENANT_CODE)
    private String tenantCode;
    @TableField(InteractionLogTable.SIGN_NO)
    private String signNo;
    @TableField(InteractionLogTable.INTERACTION_TYPE)
    private String interactionType;
    @TableField(InteractionLogTable.DIRECTION)
    private String direction;
    @TableField(InteractionLogTable.REQUEST_ID)
    private String requestId;
    @TableField(InteractionLogTable.TRACE_ID)
    private String traceId;
    @TableField(InteractionLogTable.RESULT)
    private String result;

    public InteractionLogDO() {
    }

    public InteractionLogDO(String tenantCode, String signNo, String interactionType, String requestId, String traceId) {
        this.tenantCode = tenantCode;
        this.signNo = signNo;
        this.interactionType = interactionType;
        this.direction = InteractionDirection.IN.code();
        this.requestId = requestId;
        this.traceId = traceId;
        this.result = InteractionResult.SUCCESS.code();
    }
}
