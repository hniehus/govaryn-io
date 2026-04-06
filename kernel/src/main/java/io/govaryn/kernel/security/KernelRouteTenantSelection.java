package io.govaryn.kernel.security;

/**
 * Route-derived tenant selection data for active tenant resolution.
 */
public record KernelRouteTenantSelection(
    String routeTenantId,
    boolean tenantProtectedOperation
) {
    public KernelRouteTenantSelection {
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
