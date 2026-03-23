package io.govaryn.kernel.module;

public enum ModuleLifecycleState {
    DISCOVERED,
    VALIDATED,
    REJECTED,
    REGISTERED,
    INITIALIZING,
    INITIALIZED,
    FAILED,
    DEGRADED
}
