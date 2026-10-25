package com.claim.platform.common.trace;

import java.util.Optional;
import java.util.UUID;

/**
 * Holds the request trace identifier for imperative service entry points.
 * Reactive applications must additionally propagate the identifier through their exchange/context.
 *
 * @author wangguangwu
 */
public final class TraceContext {
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String TRACE_ID_MDC_KEY = "traceId";
    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();

    private TraceContext() {
    }

    /** Returns the current identifier or creates a new opaque request identifier. */
    public static String currentOrCreate() {
        String traceId = TRACE_ID.get();
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
            TRACE_ID.set(traceId);
        }
        return traceId;
    }

    /** Binds a trusted identifier to the current execution thread. */
    public static void bind(String traceId) {
        TRACE_ID.set(traceId);
    }

    /** Reads the identifier if one has been bound. */
    public static Optional<String> current() {
        return Optional.ofNullable(TRACE_ID.get());
    }

    /** Clears thread state after request completion. */
    public static void clear() {
        TRACE_ID.remove();
    }
}
