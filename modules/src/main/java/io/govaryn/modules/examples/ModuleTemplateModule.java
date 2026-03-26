package io.govaryn.modules.examples;

import io.govaryn.kernel.api.KernelContext;
import io.govaryn.kernel.api.KernelModule;
import io.govaryn.kernel.module.ModuleCapabilities;
import io.govaryn.kernel.module.ModuleFailurePolicy;
import io.govaryn.kernel.module.ModuleMetadata;
import io.govaryn.kernel.module.ModuleType;

import java.util.List;

/**
 * Reusable starter template for new Govaryn modules.
 * This contains complete contract metadata and no domain-specific logic.
 */
public class ModuleTemplateModule implements KernelModule {

    @Override
    public ModuleMetadata metadata() {
        return new ModuleMetadata(
            "1.0.0",
            "template-module",
            "Module Template",
            "1.0.0",
            "^1.0.0",
            ModuleType.FEATURE,
            getClass().getName(),
            "Reusable starter module template",
            "Govaryn",
            "Apache-2.0",
            "https://govaryn.io/modules/template",
            new ModuleCapabilities(
                List.of("template.feature.sample"),
                List.of("template.platform.config"),
                List.of("template.platform.metrics")
            ),
            ModuleFailurePolicy.defaults(),
            null,
            List.of()
        );
    }

    @Override
    public void initialize(KernelContext context) {
        // Intentionally empty: template for future module-specific initialization.
    }
}
