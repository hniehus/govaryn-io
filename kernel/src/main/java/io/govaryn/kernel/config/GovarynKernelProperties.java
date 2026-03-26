package io.govaryn.kernel.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "govaryn.kernel")
@Validated
public class GovarynKernelProperties {

    @NotBlank(message = "govaryn.kernel.id must not be blank")
    private String id;

    @NotNull(message = "govaryn.kernel.environment must be one of: dev, stage, prod")
    private KernelEnvironment environment;

    @NotNull(message = "govaryn.kernel.module.mode must not be null")
    private ModuleMode moduleMode = ModuleMode.CLASSPATH;

    @NotBlank(message = "govaryn.kernel.module.plugin-directory must not be blank")
    private String modulePluginDirectory = "./plugins";

    @NotNull(message = "govaryn.kernel.module.failure-policy.initialization must not be null")
    private ModuleFailurePolicyAction moduleInitializationFailurePolicy = ModuleFailurePolicyAction.REJECT_MODULE_CONTINUE;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public KernelEnvironment getEnvironment() {
        return environment;
    }

    public void setEnvironment(KernelEnvironment environment) {
        this.environment = environment;
    }

    public ModuleMode getModuleMode() {
        return moduleMode;
    }

    public void setModuleMode(ModuleMode moduleMode) {
        this.moduleMode = moduleMode;
    }

    public String getModulePluginDirectory() {
        return modulePluginDirectory;
    }

    public void setModulePluginDirectory(String modulePluginDirectory) {
        this.modulePluginDirectory = modulePluginDirectory;
    }

    public ModuleFailurePolicyAction getModuleInitializationFailurePolicy() {
        return moduleInitializationFailurePolicy;
    }

    public void setModuleInitializationFailurePolicy(ModuleFailurePolicyAction moduleInitializationFailurePolicy) {
        this.moduleInitializationFailurePolicy = moduleInitializationFailurePolicy;
    }

    // Optional: Runtime validation method
    public void validateAgainstSchema() {
        // Example: Check if all required fields are set
        if (KernelConfigurationSchema.getMetadata("govaryn.kernel.id").required() && (id == null || id.isBlank())) {
            throw new IllegalArgumentException("Required key govaryn.kernel.id is missing or blank");
        }
        if (KernelConfigurationSchema.getMetadata("govaryn.kernel.environment").required() && environment == null) {
            throw new IllegalArgumentException("Required key govaryn.kernel.environment is missing");
        }
        // moduleMode has a default, so no check needed
        if (KernelConfigurationSchema.getMetadata("govaryn.kernel.module.plugin-directory").required()
            && (modulePluginDirectory == null || modulePluginDirectory.isBlank())) {
            throw new IllegalArgumentException("Required key govaryn.kernel.module.plugin-directory is missing or blank");
        }
        if (KernelConfigurationSchema.getMetadata("govaryn.kernel.module.failure-policy.initialization").required()
            && moduleInitializationFailurePolicy == null) {
            throw new IllegalArgumentException("Required key govaryn.kernel.module.failure-policy.initialization is missing");
        }
    }
}
