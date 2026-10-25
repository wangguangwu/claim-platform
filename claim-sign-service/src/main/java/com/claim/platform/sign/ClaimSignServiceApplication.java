package com.claim.platform.sign;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Runnable shell for the Sign bounded context. Domain models and APIs are intentionally deferred to sign-core.
 *
 * @author wangguangwu
 */
@SpringBootApplication
public class ClaimSignServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClaimSignServiceApplication.class, args);
    }
}
