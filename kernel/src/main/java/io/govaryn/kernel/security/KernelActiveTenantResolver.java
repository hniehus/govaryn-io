package io.govaryn.kernel.security;

import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Resolves the active tenant based on token tenant scope, explicit route tenant selection, and access policy flags.
 */
@Component
public class KernelActiveTenantResolver {

    private final KernelTenantAccessValidator tenantAccessValidator;

    public KernelActiveTenantResolver(KernelTenantAccessValidator tenantAccessValidator) {
        this.tenantAccessValidator = tenantAccessValidator;
    }

    public Optional<KernelActiveTenantContext> resolve(KernelTenantResolutionRequest request) {
        String routeTenantId = request.routeTenantId();
        KernelTenantScope tenantScope = request.tenantScope();

        if (routeTenantId != null) {
            tenantAccessValidator.validateExplicitTenantAccess(
                tenantScope,
                routeTenantId,
                request.privilegedCrossTenantAccess()
            );
            return Optional.of(new KernelActiveTenantContext(routeTenantId));
        }

        if (tenantScope.hasSingleTenant()) {
            return tenantScope.singleTenantId().map(KernelActiveTenantContext::new);
        }

        if (!request.tenantProtectedOperation()) {
            return Optional.empty();
        }

        if (tenantScope.isEmpty()) {
            throw new KernelTenantResolutionException(
                KernelTenantResolutionFailure.TENANT_CONTEXT_REQUIRED,
                "Tenant-protected operation requires tenant context"
            );
        }

        throw new KernelTenantResolutionException(
            KernelTenantResolutionFailure.EXPLICIT_TENANT_SELECTION_REQUIRED,
            "Tenant-protected operation requires an explicit route tenant selection"
        );
    }
}
