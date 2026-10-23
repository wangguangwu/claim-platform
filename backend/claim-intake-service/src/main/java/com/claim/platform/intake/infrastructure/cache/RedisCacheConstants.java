package com.claim.platform.intake.infrastructure.cache;

import java.time.format.DateTimeFormatter;

/**
 * 定义 Redis 键、Lua 脚本、结果标记和摘要算法的固定值。
 * 键结构与脚本协议属于运行期契约，值不得随重构变化。
 *
 * @author wangguangwu
 */
public final class RedisCacheConstants {

    public static final DateTimeFormatter DATE_KEY_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    public static final String SHA_256 = "SHA-256";
    public static final String KEY_SEPARATOR = "\n";

    public static final String SIGN_NO_LUA_PATH = "lua/sign-no/";
    public static final String ALLOCATE_SCRIPT = "allocate.lua";
    public static final String RECONCILE_AND_ALLOCATE_SCRIPT = "reconcile-and-allocate.lua";
    public static final String RECONCILE_ONLY_SCRIPT = "reconcile-only.lua";
    public static final long MISSING_MARKER = -1L;
    public static final long INVALID_MARKER = -2L;
    public static final int RECONCILE_ALLOCATE_RESULT_SIZE = 3;
    public static final int RECONCILE_RESULT_SIZE = 2;
    public static final int RECONCILE_ALLOCATE_SEQUENCE_INDEX = 0;
    public static final int RECONCILE_ALLOCATE_BEFORE_INDEX = 1;
    public static final int RECONCILE_ALLOCATE_AFTER_INDEX = 2;
    public static final int RECONCILE_BEFORE_INDEX = 0;
    public static final int RECONCILE_AFTER_INDEX = 1;

    public static final String RECONCILE_LOCK_SEGMENT = "reconcile-lock:";

    private RedisCacheConstants() {
    }
}
