package io.govaryn.modules.examples;

import io.govaryn.kernel.api.KernelModule;
import io.govaryn.kernel.module.ModuleMetadata;
import io.govaryn.kernel.module.ModuleType;

/**
 * Negative example: second module in a duplicate moduleId pair.
 */
public class DuplicateIdModuleB implements KernelModule {

    @Override
    public ModuleMetadata metadata() {
        return ModuleMetadata.minimal(
            "reference-duplicate-id",
            "Reference Duplicate Module B",
            "1.0.1",
            "^1.0.0",
            ModuleType.FEATURE,
            getClass().getName()
        );
    }
}
