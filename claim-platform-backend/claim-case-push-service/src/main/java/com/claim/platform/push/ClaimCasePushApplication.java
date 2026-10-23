package com.claim.platform.push;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 启动案件推送限界上下文。
 * 该上下文独立拥有案件、推送任务、回执和死信等领域事实。
 *
 * @author wangguangwu
 */
@SpringBootApplication
public class ClaimCasePushApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClaimCasePushApplication.class, args);
    }
}
