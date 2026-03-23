package io.govaryn.kernel.module;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public record ModuleCapabilities(
    List<String> providedCapabilities,
    List<String> requiredCapabilities,
    List<String> optionalCapabilities
) {
    private static final Pattern CAPABILITY_PATTERN = Pattern.compile("^[a-z][a-z0-9.:-]{2,127}$");

    public ModuleCapabilities {
        providedCapabilities = sanitizeAndValidate(providedCapabilities, "providedCapabilities");
        requiredCapabilities = sanitizeAndValidate(requiredCapabilities, "requiredCapabilities");
        optionalCapabilities = sanitizeAndValidate(optionalCapabilities, "optionalCapabilities");

        ensureDisjoint(providedCapabilities, requiredCapabilities, "providedCapabilities", "requiredCapabilities");
        ensureDisjoint(requiredCapabilities, optionalCapabilities, "requiredCapabilities", "optionalCapabilities");
    }

    public static ModuleCapabilities empty() {
        return new ModuleCapabilities(List.of(), List.of(), List.of());
    }

    private static List<String> sanitizeAndValidate(List<String> values, String fieldName) {
        if (values == null) {
            return List.of();
        }

        Set<String> unique = new LinkedHashSet<>();
        for (String value : values) {
            if (Objects.isNull(value) || value.isBlank()) {
                throw new IllegalArgumentException(fieldName + " must not contain blank values");
            }
            String trimmed = value.trim();
            if (!CAPABILITY_PATTERN.matcher(trimmed).matches()) {
                throw new IllegalArgumentException(
                    fieldName + " contains invalid capability id: " + trimmed
                );
            }
            unique.add(trimmed);
        }
        return List.copyOf(unique);
    }

    private static void ensureDisjoint(List<String> left, List<String> right, String leftName, String rightName) {
        Set<String> intersection = new LinkedHashSet<>(left);
        intersection.retainAll(right);
        if (!intersection.isEmpty()) {
            throw new IllegalArgumentException(
                leftName + " and " + rightName + " must be disjoint but overlap on " + intersection
            );
        }
    }
}
