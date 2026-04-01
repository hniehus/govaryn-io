package io.govaryn.kernel.backend.standard;

import jakarta.validation.constraints.NotBlank;

public record KernelStandardRecordWriteRequest(
    @NotBlank(message = "value must not be blank")
    String value
) {
}
