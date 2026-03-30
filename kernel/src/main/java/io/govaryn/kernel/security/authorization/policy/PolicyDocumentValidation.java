package io.govaryn.kernel.security.authorization.policy;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class PolicyDocumentValidation {

    private PolicyDocumentValidation() {
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

    static List<String> sanitizeStringList(List<String> values, String fieldName, boolean required) {
        if (values == null) {
            if (required) {
                throw new IllegalArgumentException(fieldName + " must not be null");
            }
            return List.of();
        }

        Set<String> unique = new LinkedHashSet<>();
        for (String value : values) {
            unique.add(requireNonBlank(value, fieldName + "[]"));
        }

        if (required && unique.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be empty");
        }
        return List.copyOf(unique);
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> requireObjectMap(Object value, String fieldName) {
        if (!(value instanceof Map<?, ?> rawMap)) {
            throw new IllegalArgumentException(fieldName + " must be an object");
        }
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            String key = requireNonBlank(entry.getKey() == null ? null : String.valueOf(entry.getKey()), fieldName + " key");
            map.put(key, entry.getValue());
        }
        return Map.copyOf(map);
    }

    static String requireString(Object value, String fieldName) {
        if (!(value instanceof String text)) {
            throw new IllegalArgumentException(fieldName + " must be a string");
        }
        return requireNonBlank(text, fieldName);
    }

    static List<String> requireStringList(Object value, String fieldName, boolean required) {
        if (value == null) {
            if (required) {
                throw new IllegalArgumentException(fieldName + " must not be null");
            }
            return List.of();
        }
        if (!(value instanceof List<?> rawList)) {
            throw new IllegalArgumentException(fieldName + " must be a list");
        }
        List<String> values = new java.util.ArrayList<>();
        for (Object item : rawList) {
            if (!(item instanceof String text)) {
                throw new IllegalArgumentException(fieldName + " items must be strings");
            }
            values.add(text);
        }
        return sanitizeStringList(values, fieldName, required);
    }
}
