package com.claim.platform.casedelivery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 启动 Case Delivery 限界上下文。
 * 该上下文独立拥有案件、推送任务、回执和死信等领域事实。
 *
 * @author wangguangwu
 */
@SpringBootApplication
public class ClaimCaseDeliveryApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClaimCaseDeliveryApplication.class, args);
    }
}
