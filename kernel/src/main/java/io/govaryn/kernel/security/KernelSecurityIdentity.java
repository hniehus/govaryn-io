package io.govaryn.kernel.security;

import java.util.List;

public record KernelSecurityIdentity(
    String subject,
    String issuer,
    String username,
    List<String> authorities
) {
    public KernelSecurityIdentity {
        subject = requireNonBlank(subject, "subject");
        issuer = trimToNull(issuer);
        username = trimToNull(username);
        authorities = sanitizeAuthorities(authorities);
    }

    private static String requireNonBlank(String value, String fieldName) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static List<String> sanitizeAuthorities(List<String> values) {
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
}
