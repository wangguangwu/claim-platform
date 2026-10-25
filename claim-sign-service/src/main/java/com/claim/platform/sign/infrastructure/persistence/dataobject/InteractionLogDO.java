package com.claim.platform.sign.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** Maps an append-only sanitized interaction audit row without lifecycle behavior.
 *
 * @author wangguangwu
 */
@TableName("t_claim_sign_interaction_log")
public class InteractionLogDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tenantCode;
    private String signNo;
    private String interactionType;
    private String direction;
    private String sourceSystem;
    private String targetSystem;
    private String requestId;
    private String traceId;
    private String result;
    private String errorCode;
    private String message;
    private Long costMs;
    private LocalDateTime createdAt;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTenantCode() { return tenantCode; }
    public void setTenantCode(String value) { tenantCode = value; }
    public String getSignNo() { return signNo; }
    public void setSignNo(String value) { signNo = value; }
    public String getInteractionType() { return interactionType; }
    public void setInteractionType(String value) { interactionType = value; }
    public String getDirection() { return direction; }
    public void setDirection(String value) { direction = value; }
    public String getSourceSystem() { return sourceSystem; }
    public void setSourceSystem(String value) { sourceSystem = value; }
    public String getTargetSystem() { return targetSystem; }
    public void setTargetSystem(String value) { targetSystem = value; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String value) { requestId = value; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String value) { traceId = value; }
    public String getResult() { return result; }
    public void setResult(String value) { result = value; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String value) { errorCode = value; }
    public String getMessage() { return message; }
    public void setMessage(String value) { message = value; }
    public Long getCostMs() { return costMs; }
    public void setCostMs(Long value) { costMs = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { createdAt = value; }
}
