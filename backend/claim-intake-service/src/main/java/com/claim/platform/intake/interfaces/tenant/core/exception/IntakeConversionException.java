package com.claim.platform.intake.interfaces.tenant.core.exception;

/**
 * 租户报文转换阶段可预期输入失败的基类。
 * 消息只允许包含失败类别和字段路径，不得包含字段值、Raw Payload 或完整租户 DTO。
 *
 * @author wangguangwu
 */
public class IntakeConversionException extends RuntimeException {

    private final String fieldPath;

    protected IntakeConversionException(String reason, String fieldPath) {
        this(reason, fieldPath, null);
    }

    protected IntakeConversionException(String reason, String fieldPath, Throwable cause) {
        super(reason + "，fieldPath=" + normalize(fieldPath), cause);
        this.fieldPath = normalize(fieldPath);
    }

    /**
     * 失败字段路径；无法定位具体字段时返回 {@code payload} 或 {@code request}。
     *
     * @return 字段路径
     */
    public String fieldPath() {
        return fieldPath;
    }

    private static String normalize(String fieldPath) {
        return fieldPath == null || fieldPath.isBlank() ? "payload" : fieldPath;
    }
}
