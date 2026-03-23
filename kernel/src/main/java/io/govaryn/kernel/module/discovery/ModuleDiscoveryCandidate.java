package io.govaryn.kernel.module.discovery;

import io.govaryn.kernel.module.ModuleMetadata;

import java.util.Objects;

public record ModuleDiscoveryCandidate(
    ModuleMetadata metadata,
    ModuleDiscoverySource source,
    String origin,
    boolean loadableInCurrentRuntime
) {
    public ModuleDiscoveryCandidate {
        if (Objects.isNull(metadata)) {
            throw new IllegalArgumentException("metadata must not be null");
        }
        if (Objects.isNull(source)) {
            throw new IllegalArgumentException("source must not be null");
        }
        if (Objects.isNull(origin) || origin.isBlank()) {
            throw new IllegalArgumentException("origin must not be blank");
        }
    }
}
