package com.healthcare.platform.common.observability;

import java.util.Optional;

public final class CorrelationIdHolder {
    private static final ThreadLocal<String> HOLDER = new ThreadLocal<>();

    private CorrelationIdHolder() {
    }

    public static void set(String correlationId) {
        HOLDER.set(correlationId);
    }

    public static Optional<String> get() {
        return Optional.ofNullable(HOLDER.get());
    }

    public static void clear() {
        HOLDER.remove();
    }
}
