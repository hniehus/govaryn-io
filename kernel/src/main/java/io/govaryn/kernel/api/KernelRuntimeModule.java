package io.govaryn.kernel.api;

import io.govaryn.kernel.module.ModuleMetadata;

/**
 * Formal runtime contract for kernel modules.
 */
public interface KernelRuntimeModule {

    ModuleMetadata metadata();

    default void initialize(KernelContext context) {
        // default no-op
    }

    default void start() {
        // default no-op
    }

    default void stop() {
        // default no-op
    }
}
