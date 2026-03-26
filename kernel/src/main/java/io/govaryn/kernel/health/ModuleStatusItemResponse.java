package io.govaryn.kernel.health;

public record ModuleStatusItemResponse(
    String moduleId,
    String moduleVersion,
    String moduleStatus,
    boolean degraded,
    String failurePolicy,
    String errorCause
) {
}
