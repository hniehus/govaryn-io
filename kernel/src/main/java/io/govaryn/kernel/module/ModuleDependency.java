package io.govaryn.kernel.module;

import java.util.Objects;

public record ModuleDependency(
    String moduleId,
    String versionRange,
    boolean optional
) {
    public ModuleDependency {
        if (isBlank(moduleId)) {
            throw new IllegalArgumentException("moduleId must not be blank");
        }
        if (isBlank(versionRange)) {
            throw new IllegalArgumentException("versionRange must not be blank");
        }
    }

    private static boolean isBlank(String value) {
        return Objects.isNull(value) || value.isBlank();
    }
}
