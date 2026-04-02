package io.govaryn.kernel.security;

import org.springframework.stereotype.Component;

/**
 * Validates that an explicitly requested route tenant is allowed for the current token scope.
 */
@Component
public class KernelTenantAccessValidator {

    public void validateExplicitTenantAccess(
        KernelTenantScope tenantScope,
        String routeTenantId,
        boolean privilegedCrossTenantAccess
    ) {
        if (tenantScope == null) {
            throw new IllegalArgumentException("tenantScope must not be null");
        }

        String normalizedRouteTenantId = trimToNull(routeTenantId);
        if (normalizedRouteTenantId == null) {
            throw new IllegalArgumentException("routeTenantId must not be blank");
        }

        if (tenantScope.permits(normalizedRouteTenantId)) {
            return;
        }

        if (privilegedCrossTenantAccess) {
            return;
        }

        throw new KernelTenantResolutionException(
            KernelTenantResolutionFailure.REQUESTED_TENANT_NOT_PERMITTED,
            "Requested route tenant is not within token-granted tenant scope"
        );
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
