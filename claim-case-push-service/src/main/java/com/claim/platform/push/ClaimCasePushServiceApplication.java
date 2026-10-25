package com.claim.platform.push;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Runnable shell for the Push bounded context. Push domain facts remain deferred to case-push-workflow.
 *
 * @author wangguangwu
 */
@SpringBootApplication
public class ClaimCasePushServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClaimCasePushServiceApplication.class, args);
    }
}
