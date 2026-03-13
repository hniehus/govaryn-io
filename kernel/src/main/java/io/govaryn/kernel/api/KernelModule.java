package io.govaryn.kernel.api;

public interface KernelModule {

    default String moduleName() {
        return getClass().getSimpleName();
    }

    default int order() {
        return 0;
    }

    default void init(KernelContext context) {
        // default no-op
    }

    default void start() {
        // default no-op
    }

    default void stop() {
        // default no-op
    }
}
