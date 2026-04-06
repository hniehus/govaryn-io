package io.govaryn.kernel.security;

/**
 * Result of token-driven tenant scope extraction.
 */
public record KernelTenantScopeExtraction(
    KernelTenantScope tenantScope,
    String sourceClaimKey
) {
    public KernelTenantScopeExtraction {
        if (tenantScope == null) {
            throw new IllegalArgumentException("tenantScope must not be null");
        }
        sourceClaimKey = trimToNull(sourceClaimKey);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
