package io.govaryn.modules.examples;

import io.govaryn.kernel.api.KernelContext;
import io.govaryn.kernel.api.KernelModule;
import io.govaryn.kernel.module.ModuleCapabilities;
import io.govaryn.kernel.module.ModuleFailurePolicy;
import io.govaryn.kernel.module.ModuleMetadata;
import io.govaryn.kernel.module.ModuleType;

import java.util.List;

/**
 * Support module for the reference feature module.
 * Provides a shared platform capability consumed by the reference module.
 */
public class ReferencePlatformSupportModule implements KernelModule {

    @Override
    public ModuleMetadata metadata() {
        return new ModuleMetadata(
            "1.0.0",
            "reference-platform-support",
            "Reference Platform Support Module",
            "1.0.0",
            "^1.0.0",
            ModuleType.CORE_EXTENSION,
            getClass().getName(),
            "Support capability provider for the production-like reference module",
            "Govaryn",
            "Apache-2.0",
            "https://govaryn.io/modules/reference-platform-support",
            new ModuleCapabilities(
                List.of("reference.platform.config"),
                List.of(),
                List.of()
            ),
            ModuleFailurePolicy.defaults(),
            null,
            List.of()
        );
    }

    @Override
    public void initialize(KernelContext context) {
        // Intentionally no-op for a small, deterministic support module.
    }
}
