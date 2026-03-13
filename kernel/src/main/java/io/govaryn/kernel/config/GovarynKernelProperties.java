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
}
