package com.claim.platform.sign.interfaces.tenant.core.parser;

import com.claim.platform.sign.interfaces.tenant.core.exception.IntakePayloadParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Objects;

/**
 * 基于 Jackson 的默认 JSON 解析器，按租户显式声明的目标类型反序列化。
 * 默认拒绝未知字段和尾随内容，避免租户字段被静默丢弃或报文结构被忽略。
 * 复制传入的 {@link ObjectMapper}，不修改应用共享实例。
 *
 * @param <T> 当前租户的报文 DTO 类型
 * @author wangguangwu
 */
public class DefaultJsonParser<T> implements RequestParser<T> {

    private final ObjectMapper objectMapper;
    private final Class<T> payloadType;

    public DefaultJsonParser(ObjectMapper objectMapper, Class<T> payloadType) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper").copy()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        this.payloadType = Objects.requireNonNull(payloadType, "payloadType");
    }

    @Override
    public T parse(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new IntakePayloadParseException("payload");
        }
        try {
            return objectMapper.readValue(payload, payloadType);
        } catch (JsonProcessingException exception) {
            throw new IntakePayloadParseException(fieldPath(exception), exception);
        }
    }

    private String fieldPath(JsonProcessingException exception) {
        if (!(exception instanceof JsonMappingException mappingException)) {
            return "payload";
        }
        StringBuilder path = new StringBuilder();
        for (JsonMappingException.Reference reference : mappingException.getPath()) {
            if (reference.getFieldName() != null) {
                if (!path.isEmpty()) {
                    path.append('.');
                }
                path.append(reference.getFieldName());
            } else if (reference.getIndex() >= 0) {
                path.append('[').append(reference.getIndex()).append(']');
            }
        }
        return path.isEmpty() ? "payload" : path.toString();
    }
}
