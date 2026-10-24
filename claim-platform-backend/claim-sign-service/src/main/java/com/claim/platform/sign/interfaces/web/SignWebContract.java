package com.claim.platform.sign.interfaces.web;

import java.util.regex.Pattern;

/**
 * 定义理赔签收 HTTP 入口的稳定契约：请求头、路径、字段长度和格式约束。
 * 常量值必须与既有接口逐字一致，调用方和测试不得依赖其他副本。
 *
 * @author wangguangwu
 */
public final class SignWebContract {

    public static final String DEFAULT_FIELD = "request";

    private SignWebContract() {
    }

    public static final class Headers {

        public static final String SERVICE_TOKEN = "X-Claim-Service-Token";
        public static final String TENANT = "X-Claim-Tenant";
        public static final String TRACE_ID = "X-Trace-Id";

        private Headers() {
        }
    }

    public static final class Paths {

        public static final String CLAIM_INTAKES = "/api/v1/claim-intakes";

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

        private Patterns() {
        }
    }
}
