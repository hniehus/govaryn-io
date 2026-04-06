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
    ConfigurationManager configurationManager,
    KernelAuthorizationService authorizationService,
    KernelCurrentSecurityContext currentSecurityContext
) {
    /**
     * Legacy constructor for backward compatibility without configuration manager.
     */
    public KernelContext(String kernelId, KernelEnvironment environment, String kernelVersion) {
        this(kernelId, environment, kernelVersion, null, null, null);
    }

    /**
     * Backward-compatible constructor with configuration manager only.
     */
    public KernelContext(
        String kernelId,
        KernelEnvironment environment,
        String kernelVersion,
        ConfigurationManager configurationManager
    ) {
        this(kernelId, environment, kernelVersion, configurationManager, null, null);
    }

    /**
     * Backward-compatible constructor with configuration manager and authorization service.
     */
    public KernelContext(
        String kernelId,
        KernelEnvironment environment,
        String kernelVersion,
        ConfigurationManager configurationManager,
        KernelAuthorizationService authorizationService
    ) {
        this(
            kernelId,
            environment,
            kernelVersion,
            configurationManager,
            authorizationService,
            null
        );
    }
}
