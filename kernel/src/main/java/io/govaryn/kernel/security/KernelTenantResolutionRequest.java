package io.govaryn.kernel.security;

/**
 * Input for active tenant resolution based on token scope and explicit route tenant selection.
 */
public record KernelTenantResolutionRequest(
    KernelTenantScope tenantScope,
    String routeTenantId,
    boolean tenantProtectedOperation,
    boolean privilegedCrossTenantAccess
) {
    public KernelTenantResolutionRequest {
        if (tenantScope == null) {
            throw new IllegalArgumentException("tenantScope must not be null");
        }
        routeTenantId = trimToNull(routeTenantId);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
