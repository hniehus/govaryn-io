package io.govaryn.modules.examples;

import io.govaryn.kernel.api.KernelContext;
import io.govaryn.kernel.api.KernelModule;
import io.govaryn.kernel.module.ModuleMetadata;
import io.govaryn.kernel.module.ModuleType;

/**
 * Minimal valid reference module for local development and onboarding.
 */
public class MinimalReferenceModule implements KernelModule {

    @Override
    public ModuleMetadata metadata() {
        return ModuleMetadata.minimal(
            "reference-minimal",
            "Reference Minimal Module",
            "1.0.0",
            "^1.0.0",
            ModuleType.FEATURE,
            getClass().getName()
        );
    }

    @Override
    public void initialize(KernelContext context) {
        // Intentionally minimal no-op initialization.
    }
}
