package io.govaryn.modules.examples;

import io.govaryn.kernel.api.KernelContext;
import io.govaryn.kernel.api.KernelModule;
import io.govaryn.kernel.module.ModuleCapabilities;
import io.govaryn.kernel.module.ModuleFailurePolicy;
import io.govaryn.kernel.module.ModuleMetadata;
import io.govaryn.kernel.module.ModuleType;

import java.util.List;

/**
 * Support module used by the reusable template profile.
 * It provides a shared capability consumed by ModuleTemplateModule.
 */
public class ModuleTemplateSupportModule implements KernelModule {

    @Override
    public ModuleMetadata metadata() {
        return new ModuleMetadata(
            "1.0.0",
            "template-support",
            "Module Template Support",
            "1.0.0",
            "^1.0.0",
            ModuleType.CORE_EXTENSION,
            getClass().getName(),
            "Capability provider for the reusable module template",
            "Govaryn",
            "Apache-2.0",
            "https://govaryn.io/modules/template-support",
            new ModuleCapabilities(
                List.of("template.platform.config"),
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
        // Template support module intentionally keeps initialization as a no-op.
    }
}
