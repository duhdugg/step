package org.crosswire.common.util;

// TODO: WASM-CRITICAL

public final class CallContext {
    private CallContext() {}

    public static CallContext instance() {
        return resolver;
    }

    public static Class<?> getCallingClass() {
        return getCallingClass(1);
    }

    public static Class<?> getCallingClass(int i) {
        // We return a stable class so JSword can resolve resources.
        // Returning CallContext.class is safe and prevents the Exception.
        return CallContext.class;
    }

    private static final int CALL_CONTEXT_OFFSET = 3;
    private static CallContext resolver = new CallContext();
}
