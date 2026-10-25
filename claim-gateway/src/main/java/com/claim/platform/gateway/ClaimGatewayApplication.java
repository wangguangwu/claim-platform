package com.claim.platform.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Edge application for routing, trusted local development identity, rate limiting and trace propagation.
 * It intentionally contains no claim-domain decisions.
 *
 * @author wangguangwu
 */
@SpringBootApplication
public class ClaimGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClaimGatewayApplication.class, args);
    }
}
