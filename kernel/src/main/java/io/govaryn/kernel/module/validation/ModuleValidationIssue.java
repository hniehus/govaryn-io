package io.govaryn.kernel.module.validation;

public record ModuleValidationIssue(
    ModuleValidationSeverity severity,
    ModuleValidationCode code,
    String fieldPath,
    String message
) {
}
