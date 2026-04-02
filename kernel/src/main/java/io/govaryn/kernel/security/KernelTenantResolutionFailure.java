package io.govaryn.kernel.security;

public enum KernelTenantResolutionFailure {
    TENANT_CONTEXT_REQUIRED,
    EXPLICIT_TENANT_SELECTION_REQUIRED,
    REQUESTED_TENANT_NOT_PERMITTED
}
