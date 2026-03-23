package io.govaryn.kernel.examples.modules;

import io.govaryn.kernel.api.KernelModule;
import io.govaryn.kernel.module.ModuleMetadata;
import io.govaryn.kernel.module.ModuleType;

/**
 * Negative example: first module in a duplicate moduleId pair.
 */
public class DuplicateIdModuleA implements KernelModule {

    @Override
    public ModuleMetadata metadata() {
        return ModuleMetadata.minimal(
            "reference-duplicate-id",
            "Reference Duplicate Module A",
            "1.0.0",
            "^1.0.0",
            ModuleType.FEATURE,
            getClass().getName()
        );
    }
}
