package io.govaryn.kernel.api;

import io.govaryn.kernel.config.KernelEnvironment;

public record KernelContext(String kernelId, KernelEnvironment environment, String kernelVersion) {
}
