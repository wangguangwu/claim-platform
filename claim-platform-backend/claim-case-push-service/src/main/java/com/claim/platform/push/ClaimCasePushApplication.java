package com.claim.platform.push;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 启动案件推送限界上下文。
 * 该上下文独立拥有推送任务、记录、回执和策略等领域事实；案件业务事实由内部案件系统拥有。
 *
 * @author wangguangwu
 */
@SpringBootApplication
public class ClaimCasePushApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClaimCasePushApplication.class, args);
    }
}
