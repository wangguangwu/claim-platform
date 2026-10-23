package com.claim.platform.intake.application.service;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.UUID;

/**
 * 生成平台签收编号，编号不读取或拼接任何租户报文内容。
 *
 * @author wangguangwu
 */
@Component
public class SignNoGenerator {

    private static final String SIGN_NO_PREFIX = "SIGN";
    private static final int RANDOM_HEX_LENGTH = 20;

    /**
     * 生成 {@code SIGN} 前缀加 20 位大写十六进制字符的签收编号。
     *
     * @return 平台签收编号
     */
    public String generate() {
        String randomHex = UUID.randomUUID().toString().replace("-", "");
        return SIGN_NO_PREFIX + randomHex.substring(0, RANDOM_HEX_LENGTH).toUpperCase(Locale.ROOT);
    }
}
