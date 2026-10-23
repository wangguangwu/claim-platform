package com.claim.platform.sign;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 启动理赔签收限界上下文。
 * 后续申请签收、材料处理和下游同步能力只能在该上下文拥有的数据边界内实现。
 *
 * @author wangguangwu
 */
@SpringBootApplication
public class ClaimSignApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClaimSignApplication.class, args);
    }
}
