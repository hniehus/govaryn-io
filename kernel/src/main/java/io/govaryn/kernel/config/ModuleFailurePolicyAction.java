package io.govaryn.kernel.config;

public enum ModuleFailurePolicyAction {
    FAIL_FAST,
    REJECT_MODULE_CONTINUE,
    MARK_MODULE_DEGRADED
}
