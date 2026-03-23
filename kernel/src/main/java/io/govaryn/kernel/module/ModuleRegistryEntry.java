package io.govaryn.kernel.module;

import java.util.Objects;

public record ModuleRegistryEntry(
    ModuleMetadata metadata,
    ModuleStatus status,
    boolean mandatory,
    String origin
) {
    public ModuleRegistryEntry {
        if (Objects.isNull(metadata)) {
            throw new IllegalArgumentException("metadata must not be null");
        }
        if (Objects.isNull(status)) {
            throw new IllegalArgumentException("status must not be null");
        }
        if (Objects.isNull(origin) || origin.isBlank()) {
            throw new IllegalArgumentException("origin must not be blank");
        }
    }

    public ModuleRegistryEntry withStatus(ModuleStatus nextStatus) {
        return new ModuleRegistryEntry(metadata, nextStatus, mandatory, origin);
    }
}
