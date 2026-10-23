package com.claim.platform.intake.interfaces.tenant.core.context;

import java.util.Optional;

/**
 * 按请求线程承载可信租户上下文。
 * 入口适配器必须在调用转换层前写入，并在请求结束时调用 {@link #clear()}，避免线程池复用导致租户串扰。
 *
 * @author wangguangwu
 */
public final class TenantContextHolder {

    private static final ThreadLocal<TenantContext> CONTEXT = new ThreadLocal<>();

    private TenantContextHolder() {
    }

    /**
     * 写入当前请求的可信租户上下文。
     *
     * @param context 不可变租户上下文快照
     */
    public static void set(TenantContext context) {
        if (context == null) {
            throw new IllegalArgumentException("租户上下文不能为空");
        }
        CONTEXT.set(context);
    }

    /**
     * 读取当前请求的可信租户上下文。
     *
     * @return 上下文；未写入时为空
     */
    public static Optional<TenantContext> get() {
        return Optional.ofNullable(CONTEXT.get());
    }

    /**
     * 清理当前线程的租户上下文，必须在请求结束时执行。
     */
    public static void clear() {
        CONTEXT.remove();
    }
}
