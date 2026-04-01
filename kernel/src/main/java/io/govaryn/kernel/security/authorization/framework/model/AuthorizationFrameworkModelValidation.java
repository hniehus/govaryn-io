package io.govaryn.kernel.security.authorization.framework.model;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class AuthorizationFrameworkModelValidation {

    private AuthorizationFrameworkModelValidation() {
    }

    static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    static <T> T requireNonNull(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
        return value;
    }

    static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    static List<String> sanitizeStringList(List<String> values, String fieldName) {
        if (values == null) {
            return List.of();
        }
        Set<String> unique = new LinkedHashSet<>();
        for (String value : values) {
            unique.add(requireNonBlank(value, fieldName + "[]"));
        }
        return List.copyOf(unique);
    }

    static Map<String, String> sanitizeStringMap(Map<String, String> values, String fieldName) {
        if (values == null) {
            return Map.of();
        }
        Map<String, String> sanitized = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String key = requireNonBlank(entry.getKey(), fieldName + " key");
            String mapValue = entry.getValue();
            if (mapValue == null) {
                throw new IllegalArgumentException(fieldName + " value must not be null");
            }
            sanitized.put(key, mapValue.trim());
        }
        return Map.copyOf(sanitized);
    }
}
