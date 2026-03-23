package io.govaryn.kernel.module;

import java.util.Objects;

public record ModuleFailureDetails(
    String errorCode,
    String message
) {
    public ModuleFailureDetails {
        if (Objects.isNull(errorCode) || errorCode.isBlank()) {
            throw new IllegalArgumentException("errorCode must not be blank");
        }
        if (Objects.isNull(message) || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
    }
}
