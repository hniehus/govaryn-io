package io.govaryn.kernel.module;

import io.govaryn.kernel.config.ModuleFailurePolicyAction;

import java.util.Objects;

public record ModuleFailurePolicy(
    ModuleFailurePolicyAction onInitializationFailure,
    ModuleFailurePolicyAction onRuntimeFailure
) {
    public ModuleFailurePolicy {
        if (Objects.isNull(onInitializationFailure)) {
            throw new IllegalArgumentException("onInitializationFailure must not be null");
        }
        if (Objects.isNull(onRuntimeFailure)) {
            throw new IllegalArgumentException("onRuntimeFailure must not be null");
        }
    }

    public static ModuleFailurePolicy defaults() {
        return new ModuleFailurePolicy(
            ModuleFailurePolicyAction.REJECT_MODULE_CONTINUE,
            ModuleFailurePolicyAction.MARK_MODULE_DEGRADED
        );
    }
}
