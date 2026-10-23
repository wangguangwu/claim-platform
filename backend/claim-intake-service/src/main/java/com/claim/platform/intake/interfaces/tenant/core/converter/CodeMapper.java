package com.claim.platform.intake.interfaces.tenant.core.converter;

import com.claim.platform.intake.interfaces.tenant.core.exception.IntakeCodeMappingException;
import java.util.HashMap;
import java.util.Map;

/**
 * 将租户私有编码收敛为平台标准枚举的不可变映射器。
 * 映射表由租户组件显式声明；未知编码必须失败并报告字段路径，不得透传原值。
 *
 * @param <E> 平台标准枚举类型
 * @author wangguangwu
 */
public final class CodeMapper<E extends Enum<E>> {

    private final String fieldPath;
    private final Map<String, E> mappings;

    public CodeMapper(String fieldPath, Map<String, E> mappings) {
        if (fieldPath == null || fieldPath.isBlank()) {
            throw new IllegalArgumentException("fieldPath 不能为空");
        }
        if (mappings == null || mappings.isEmpty()) {
            throw new IllegalArgumentException("映射表不能为空，fieldPath=" + fieldPath);
        }
        Map<String, E> copied = new HashMap<>();
        for (Map.Entry<String, E> entry : mappings.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null) {
                throw new IllegalArgumentException("映射表包含非法项，fieldPath=" + fieldPath);
            }
            copied.put(entry.getKey(), entry.getValue());
        }
        this.fieldPath = fieldPath;
        this.mappings = Map.copyOf(copied);
    }

    /**
     * 映射租户编码。
     *
     * @param tenantCode 租户原始编码
     * @return 平台标准枚举
     * @throws IntakeCodeMappingException 编码未在映射表中声明
     */
    public E map(String tenantCode) {
        E mapped = tenantCode == null ? null : mappings.get(tenantCode);
        if (mapped == null) {
            throw new IntakeCodeMappingException(fieldPath);
        }
        return mapped;
    }
}
