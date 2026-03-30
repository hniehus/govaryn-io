package io.govaryn.kernel.api;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public record KernelAuthorizationOperation(
    String action,
    String resourceType,
    String resourceId,
    Map<String, String> context
) {
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-z][a-z0-9._:-]{1,63}$");
    private static final Set<String> SUPPORTED_CONTEXT_KEYS = Set.of("environment");

    public KernelAuthorizationOperation {
        action = requireNonBlank(action, "action");
        resourceType = requireNonBlank(resourceType, "resourceType");
        resourceId = resourceId == null ? null : requireNonBlank(resourceId, "resourceId");
        context = sanitizeContext(context);

        if (!NAME_PATTERN.matcher(action).matches()) {
            throw new IllegalArgumentException("action must match ^[a-z][a-z0-9._:-]{1,63}$");
        }
        if (!NAME_PATTERN.matcher(resourceType).matches()) {
            throw new IllegalArgumentException("resourceType must match ^[a-z][a-z0-9._:-]{1,63}$");
        }
    }

    public static List<String> supportedContextKeys() {
        return List.copyOf(SUPPORTED_CONTEXT_KEYS);
    }

    private static Map<String, String> sanitizeContext(Map<String, String> context) {
        if (context == null) {
            return Map.of();
        }
        Map<String, String> sanitized = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, String> entry : context.entrySet()) {
            String key = requireNonBlank(entry.getKey(), "context key");
            String value = requireNonBlank(entry.getValue(), "context value");
            if (!SUPPORTED_CONTEXT_KEYS.contains(key)) {
                throw new IllegalArgumentException(
                    "Unsupported context key '" + key + "'. Supported keys: " + SUPPORTED_CONTEXT_KEYS
                );
            }
            sanitized.put(key, value);
        }
        return Map.copyOf(sanitized);
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
