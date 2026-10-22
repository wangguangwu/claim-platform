package com.claim.platform.intake;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 启动 Claim Intake 限界上下文。
 * 后续申请签收、材料处理和下游同步能力只能在该上下文拥有的数据边界内实现。
 *
 * @author wangguangwu
 */
@SpringBootApplication
public class ClaimIntakeApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClaimIntakeApplication.class, args);
    }
}
