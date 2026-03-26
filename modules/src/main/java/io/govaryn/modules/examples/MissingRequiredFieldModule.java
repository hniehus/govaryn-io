package io.govaryn.modules.examples;

import io.govaryn.kernel.api.KernelModule;
import io.govaryn.kernel.module.ModuleMetadata;
import io.govaryn.kernel.module.ModuleType;

/**
 * Negative example: invalid module metadata with a missing required field (moduleName).
 */
public class MissingRequiredFieldModule implements KernelModule {

    @Override
    public ModuleMetadata metadata() {
        return ModuleMetadata.minimal(
            "reference-missing-required-field",
            " ",
            "1.0.0",
            "^1.0.0",
            ModuleType.FEATURE,
            getClass().getName()
        );
    }
}
