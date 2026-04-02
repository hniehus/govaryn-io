package io.govaryn.kernel.security;

import java.util.List;
import java.util.Optional;

/**
 * Token-derived tenant scope granted to the authenticated principal.
 */
public record KernelTenantScope(List<String> permittedTenantIds) {

    public KernelTenantScope {
        permittedTenantIds = sanitizeTenantIds(permittedTenantIds);
    }

    public static KernelTenantScope empty() {
        return new KernelTenantScope(List.of());
    }

    public boolean isEmpty() {
        return permittedTenantIds.isEmpty();
    }

    public boolean hasSingleTenant() {
        return permittedTenantIds.size() == 1;
    }

    public Optional<String> singleTenantId() {
        if (permittedTenantIds.size() != 1) {
            return Optional.empty();
        }
        return Optional.of(permittedTenantIds.getFirst());
    }

    public boolean permits(String tenantId) {
        String normalized = trimToNull(tenantId);
        return normalized != null && permittedTenantIds.contains(normalized);
    }

    private static List<String> sanitizeTenantIds(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
            .filter(value -> value != null && !value.isBlank())
            .map(String::trim)
            .distinct()
            .sorted()
            .toList();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
