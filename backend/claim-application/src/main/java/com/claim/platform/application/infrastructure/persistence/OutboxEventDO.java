package com.claim.platform.application.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 保存待异步投递的签收事件意图，发布器将在后续功能中消费此表。
 *
 * @author wangguangwu
 */
@TableName("t_sign_outbox")
public class OutboxEventDO {
    @TableId(type = IdType.AUTO) private Long id;
    @TableField("event_id") private String eventId;
    @TableField("event_type") private String eventType;
    @TableField("event_version") private String eventVersion;
    @TableField("tenant_code") private String tenantCode;
    @TableField("aggregate_type") private String aggregateType;
    @TableField("aggregate_id") private String aggregateId;
    private String payload;
    @TableField("trace_id") private String traceId;
    private String status;
    @TableField("occurred_at") private LocalDateTime occurredAt;

    public OutboxEventDO() {
    }

    public OutboxEventDO(String eventId, String eventType, String eventVersion, String tenantCode, String aggregateType,
                      String aggregateId, String payload, String traceId, LocalDateTime occurredAt) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.eventVersion = eventVersion;
        this.tenantCode = tenantCode;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.payload = payload;
        this.traceId = traceId;
        this.status = "PENDING";
        this.occurredAt = occurredAt;
    }
}
