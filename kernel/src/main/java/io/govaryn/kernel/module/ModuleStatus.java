package io.govaryn.kernel.module;

import java.time.Instant;
import java.util.Objects;

public record ModuleStatus(
    ModuleLifecycleState lifecycleState,
    boolean degraded,
    Instant stateUpdatedAt,
    ModuleFailureDetails lastError
) {
    public ModuleStatus {
        if (Objects.isNull(lifecycleState)) {
            throw new IllegalArgumentException("lifecycleState must not be null");
        }
        if (Objects.isNull(stateUpdatedAt)) {
            throw new IllegalArgumentException("stateUpdatedAt must not be null");
        }
    }

    public static ModuleStatus discovered() {
        return new ModuleStatus(ModuleLifecycleState.DISCOVERED, false, Instant.now(), null);
    }

    public static ModuleStatus withState(ModuleLifecycleState state, ModuleFailureDetails errorDetails) {
        return new ModuleStatus(state, false, Instant.now(), errorDetails);
    }

    public static ModuleStatus degraded(ModuleFailureDetails errorDetails) {
        return new ModuleStatus(ModuleLifecycleState.DEGRADED, true, Instant.now(), errorDetails);
    }
}
