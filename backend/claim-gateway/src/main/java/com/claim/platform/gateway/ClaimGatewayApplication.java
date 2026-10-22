package com.claim.platform.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 启动边缘层服务；该模块仅提供认证、租户识别、路由和流量治理入口，
 * 不承载领域状态或领域编排。
 *
 * @author wangguangwu
 */
@SpringBootApplication
public class ClaimGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClaimGatewayApplication.class, args);
    }
}
