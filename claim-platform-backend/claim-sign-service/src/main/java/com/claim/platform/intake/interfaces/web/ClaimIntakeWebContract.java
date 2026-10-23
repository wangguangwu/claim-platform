package com.claim.platform.intake.interfaces.web;

import java.util.regex.Pattern;

/**
 * 定义 Claim Intake HTTP 入口的稳定契约：请求头、路径、字段长度和格式约束。
 * 常量值必须与既有接口逐字一致，调用方和测试不得依赖其他副本。
 *
 * @author wangguangwu
 */
public final class ClaimIntakeWebContract {

    public static final String DEFAULT_FIELD = "request";

    private ClaimIntakeWebContract() {
    }

    public static final class Headers {

        public static final String SERVICE_TOKEN = "X-Claim-Service-Token";
        public static final String TENANT = "X-Claim-Tenant";
        public static final String TRACE_ID = "X-Trace-Id";
        public static final String PROTOCOL_VERSION = "X-Claim-Protocol-Version";

        private Headers() {
        }
    }

    public static final class Paths {

        public static final String CLAIM_INTAKES = "/api/v1/claim-intakes";
        public static final String TENANT_INTAKES = "/api/v1/tenant-intakes";

        private Paths() {
        }
    }

    public static final class Limits {

        public static final int IDEMPOTENCY_KEY_LENGTH = 64;
        public static final int APPLICANT_NAME_LENGTH = 64;
        public static final int SIGN_NO_LENGTH = 64;
        public static final int BATCH_NO_LENGTH = 64;
        public static final int MATERIAL_TYPE_LENGTH = 32;
        public static final int MATERIAL_NAME_LENGTH = 255;
        public static final int MATERIAL_PATH_LENGTH = 1000;

        private Limits() {
        }
    }

    public static final class Patterns {

        public static final Pattern TENANT_CODE = Pattern.compile("[A-Za-z0-9_-]{1,64}");
        public static final Pattern PROTOCOL_VERSION = Pattern.compile("[A-Za-z0-9._-]{1,32}");

        private Patterns() {
        }
    }
}
