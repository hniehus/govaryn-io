package io.govaryn.modules.examples;

import io.govaryn.kernel.api.KernelContext;
import io.govaryn.kernel.api.KernelModule;
import io.govaryn.kernel.module.ModuleCapabilities;
import io.govaryn.kernel.module.ModuleFailurePolicy;
import io.govaryn.kernel.module.ModuleMetadata;
import io.govaryn.kernel.module.ModuleType;

import java.util.List;

/**
 * Production-like reference module built on top of the module template.
 * Demonstrates realistic metadata and capability usage while staying small.
 */
public class ReferenceFeatureModule implements KernelModule {

    private final boolean failDuringInitialize;

    public ReferenceFeatureModule() {
        this(false);
    }

    public ReferenceFeatureModule(boolean failDuringInitialize) {
        this.failDuringInitialize = failDuringInitialize;
    }

    @Override
    public ModuleMetadata metadata() {
        return new ModuleMetadata(
            "1.0.0",
            "reference-feature",
            "Reference Feature Module",
            "1.0.0",
            "^1.0.0",
            ModuleType.FEATURE,
            getClass().getName(),
            "Production-like reference module for kernel integration patterns",
            "Govaryn",
            "Apache-2.0",
            "https://govaryn.io/modules/reference-feature",
            new ModuleCapabilities(
                List.of("reference.feature.sample"),
                List.of("reference.platform.config"),
                List.of("reference.platform.metrics")
            ),
            ModuleFailurePolicy.defaults(),
            null,
            List.of()
        );
    }

    @Override
    public void initialize(KernelContext context) {
        // Controlled failure path for integration tests and diagnostics.
        if (failDuringInitialize) {
            throw new IllegalStateException(
                "Reference feature module forced initialization failure for testing"
            );
        }
    }
}
