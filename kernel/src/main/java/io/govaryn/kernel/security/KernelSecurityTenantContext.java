package io.govaryn.kernel.security;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Kernel-managed request context that combines authenticated principal and tenant context.
 */
public record KernelSecurityTenantContext(
    KernelSecurityIdentity principal,
    KernelTenantScope tenantScope,
    KernelActiveTenantContext activeTenant,
    boolean privilegedCrossTenantAccess,
    Map<String, String> claims,
    Map<String, String> authenticationMetadata
) {
    public KernelSecurityTenantContext {
        principal = requireNonNull(principal, "principal");
        tenantScope = requireNonNull(tenantScope, "tenantScope");
        claims = sanitizeStringMap(claims);
        authenticationMetadata = sanitizeStringMap(authenticationMetadata);

        if (activeTenant != null
            && !tenantScope.permits(activeTenant.tenantId())
            && !privilegedCrossTenantAccess) {
            throw new IllegalArgumentException("activeTenant must be inside tenantScope");
        }
    }

    public String userId() {
        return principal.subject();
    }

    public List<String> authorities() {
        return principal.authorities();
    }

    public String activeTenantId() {
        return activeTenant == null ? null : activeTenant.tenantId();
    }

    private static <T> T requireNonNull(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
        return value;
    }

    private static Map<String, String> sanitizeStringMap(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, String> sanitized = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (entry == null) {
                continue;
            }
            String key = trimToNull(entry.getKey());
            String value = trimToNull(entry.getValue());
            if (key != null && value != null) {
                sanitized.put(key, value);
            }
        }
        return Map.copyOf(sanitized);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
