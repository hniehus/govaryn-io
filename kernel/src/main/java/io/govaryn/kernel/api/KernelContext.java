package io.govaryn.kernel.api;

import io.govaryn.kernel.config.ConfigurationManager;
import io.govaryn.kernel.config.KernelEnvironment;

/**
 * Central context providing modules access to kernel services.
 * Includes configuration management, environment, and metadata.
 */
public record KernelContext(
    String kernelId,
    KernelEnvironment environment,
    String kernelVersion,
    ConfigurationManager configurationManager
) {
    /**
     * Legacy constructor for backward compatibility without configuration manager.
     */
    public KernelContext(String kernelId, KernelEnvironment environment, String kernelVersion) {
        this(kernelId, environment, kernelVersion, null);
    }
}
