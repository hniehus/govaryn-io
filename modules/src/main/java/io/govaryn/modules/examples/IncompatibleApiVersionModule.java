package io.govaryn.modules.examples;

import io.govaryn.kernel.api.KernelModule;
import io.govaryn.kernel.module.ModuleMetadata;
import io.govaryn.kernel.module.ModuleType;

/**
 * Negative example: module requiring an incompatible kernel API version.
 */
public class IncompatibleApiVersionModule implements KernelModule {

    @Override
    public ModuleMetadata metadata() {
        return ModuleMetadata.minimal(
            "reference-incompatible-api",
            "Reference Incompatible API Module",
            "1.0.0",
            "^9.0.0",
            ModuleType.FEATURE,
            getClass().getName()
        );
    }
}
