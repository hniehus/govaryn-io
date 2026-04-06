package io.govaryn.kernel.api;

import io.govaryn.kernel.security.KernelActiveTenantContext;
import io.govaryn.kernel.security.KernelSecurityIdentity;
import io.govaryn.kernel.security.KernelSecurityTenantContext;
import io.govaryn.kernel.security.KernelTenantScope;

import java.util.Optional;

/**
 * Kernel-facing API for accessing the current authenticated security and tenant context.
 */
public interface KernelCurrentSecurityContext {

    Optional<KernelSecurityTenantContext> currentKernelContext();

    default Optional<KernelSecurityIdentity> currentPrincipal() {
        return currentKernelContext().map(KernelSecurityTenantContext::principal);
    }

    default Optional<KernelTenantScope> currentTenantScope() {
        return currentKernelContext().map(KernelSecurityTenantContext::tenantScope);
    }

    default Optional<KernelActiveTenantContext> currentActiveTenant() {
        return currentKernelContext().map(KernelSecurityTenantContext::activeTenant);
    }
}
