package com.claim.platform.intake.application.service;

/**
 * 定义 Claim Intake 指标名称、标签键、标签值和结果码。
 * 指标契约属于外部可观测接口，值不得随重构变化。
 *
 * @author wangguangwu
 */
public final class IntakeMetricConstants {

    public static final String SUBMISSION_METRIC = "claim.intake.submission";
    public static final String REPLAY_CACHE_METRIC = "claim.intake.submission.replay-cache";
    public static final String SUBMISSION_LOCK_METRIC = "claim.intake.submission.lock";
    public static final String SIGN_NO_ALLOCATION_METRIC = "claim.intake.number.allocation";
    public static final String SIGN_NO_FALLBACK_METRIC = "claim.intake.number.fallback";
    public static final String SIGN_NO_RECOVERY_METRIC = "claim.intake.number.recovery";
    public static final String SIGN_NO_CONFLICT_METRIC = "claim.intake.number.conflict";
    public static final String SIGN_NO_SEQUENCE_GAP_METRIC = "claim.intake.number.sequence_gap";

    public static final String TAG_SOURCE = "source";
    public static final String TAG_RESULT = "result";
    public static final String TAG_TRIGGER = "trigger";
    public static final String TAG_TYPE = "type";
    public static final String TAG_SCOPE = "scope";
    public static final String TAG_OPERATION = "operation";

    public static final String OPERATION_READ = "read";
    public static final String OPERATION_WRITE = "write";
    public static final String OPERATION_EVICT = "evict";

    public static final String RESULT_SUCCESS = "success";
    public static final String RESULT_SKIPPED = "skipped";
    public static final String RESULT_FAILURE = "failure";
    public static final String RESULT_ALLOCATED = "allocated";
    public static final String RESULT_RECOVERED = "recovered";
    public static final String RESULT_EXHAUSTED = "exhausted";
    public static final String RESULT_UNAVAILABLE = "unavailable";
    public static final String RESULT_CONFLICT = "conflict";
    public static final String RESULT_CREATED = "created";
    public static final String RESULT_REPLAYED_QUICK = "replayed_quick";
    public static final String RESULT_REPLAYED_LOCKED = "replayed_locked";
    public static final String RESULT_REPLAYED_CONFLICT = "replayed_conflict";
    public static final String RESULT_LOCK_TIMEOUT = "lock_timeout";
    public static final String RESULT_LOCK_DEGRADED = "lock_degraded";
    public static final String RESULT_ERROR = "error";
    public static final String RESULT_HIT = "hit";
    public static final String RESULT_MISS = "miss";
    public static final String RESULT_ACQUIRED = "acquired";
    public static final String RESULT_TIMEOUT = "timeout";

    public static final String CONFLICT_EXTERNAL_APPLY_NO = "external_apply_no";
    public static final String CONFLICT_SIGN_NO = "sign_no";

    public static final String LAYER_QUICK_QUERY = "quick-query";
    public static final String LAYER_LOCKED_DOUBLE_CHECK = "locked-double-check";
    public static final String LAYER_UNIQUE_CONFLICT = "unique-conflict";

    public static final String SCOPE_CURRENT = "current";
    public static final String SCOPE_PREVIOUS = "previous";

    private IntakeMetricConstants() {
    }
}
