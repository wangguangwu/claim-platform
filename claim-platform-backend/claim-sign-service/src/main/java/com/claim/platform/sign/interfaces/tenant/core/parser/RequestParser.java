package com.claim.platform.sign.interfaces.tenant.core.parser;

/**
 * 将 Raw Payload 解析为当前租户专属 DTO，只负责报文结构解析。
 * 实现不得执行字段校验、构造应用命令或访问持久化。
 *
 * @param <T> 当前租户的报文 DTO 类型
 * @author wangguangwu
 */
public interface RequestParser<T> {

    /**
     * 解析租户报文。
     *
     * @param payload 已完成验签和解密的原始报文
     * @return 租户 DTO
     */
    T parse(String payload);
}
