package io.govaryn.kernel.examples.modules;

import io.govaryn.kernel.api.KernelContext;
import io.govaryn.kernel.api.KernelModule;
import io.govaryn.kernel.module.ModuleMetadata;
import io.govaryn.kernel.module.ModuleType;

/**
 * Negative example: module that fails during initialization in a controlled way.
 */
public class FailingInitializationModule implements KernelModule {

    @Override
    public ModuleMetadata metadata() {
        return ModuleMetadata.minimal(
            "reference-failing-init",
            "Reference Failing Init Module",
            "1.0.0",
            "^1.0.0",
            ModuleType.FEATURE,
            getClass().getName()
        );
    }

    @Override
    public void initialize(KernelContext context) {
        throw new IllegalStateException("Reference module forced initialization failure");
    }
}
