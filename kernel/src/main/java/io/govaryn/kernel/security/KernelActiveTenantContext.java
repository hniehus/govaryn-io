package io.govaryn.kernel.security;

/**
 * Active tenant resolved for the current request.
 */
public record KernelActiveTenantContext(String tenantId) {

    public KernelActiveTenantContext {
        tenantId = requireNonBlank(tenantId, "tenantId");
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
