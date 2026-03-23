package io.govaryn.kernel.module.validation;

import io.govaryn.kernel.module.discovery.ModuleDiscoverySource;

import java.util.List;
import java.util.Objects;

public record ModuleValidationReport(
    String moduleId,
    String moduleName,
    ModuleDiscoverySource source,
    boolean valid,
    List<ModuleValidationIssue> issues
) {
    public ModuleValidationReport {
        Objects.requireNonNull(source, "source must not be null");
        issues = issues == null ? List.of() : List.copyOf(issues);
    }
}
