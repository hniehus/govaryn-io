package io.govaryn.kernel.internal;

import io.govaryn.kernel.config.ConfigurationException;
import io.govaryn.kernel.config.KernelConfigurationSchema;
import io.govaryn.kernel.config.ModuleConfigurationSchema;

import java.util.List;
import java.util.Map;

/**
 * Example module configuration schema demonstrating how modules declare their configuration.
 * This serves as a reference implementation for other modules.
 */
public class ExampleModuleConfigurationSchema implements ModuleConfigurationSchema {

    @Override
    public String getNamespace() {
        return "govaryn.example";
    }

    @Override
    public List<KernelConfigurationSchema.ConfigKeyMetadata> getKeyMetadata() {
        return List.of(
            new KernelConfigurationSchema.ConfigKeyMetadata(
                "govaryn.example.enabled",
                KernelConfigurationSchema.ConfigType.ENUM,
                "true", // Default value
                true,   // Required
                KernelConfigurationSchema.Strictness.STRICT,
                List.of("true", "false")
            ),
            new KernelConfigurationSchema.ConfigKeyMetadata(
                "govaryn.example.database.host",
                KernelConfigurationSchema.ConfigType.STRING,
                "localhost",
                false, // Optional
                KernelConfigurationSchema.Strictness.NON_STRICT,
                List.of()
            ),
            new KernelConfigurationSchema.ConfigKeyMetadata(
                "govaryn.example.database.password",
                KernelConfigurationSchema.ConfigType.STRING,
                null,
                false, // Optional
                KernelConfigurationSchema.Strictness.NON_STRICT,
                List.of()
                // Note: Key name contains "password", so it will be auto-redacted by SecretRedactor
            ),
            new KernelConfigurationSchema.ConfigKeyMetadata(
                "govaryn.example.connection.timeout",
                KernelConfigurationSchema.ConfigType.STRING,
                "30", // Default in seconds
                false,
                KernelConfigurationSchema.Strictness.NON_STRICT,
                List.of()
            )
        );
    }

    @Override
    public void validateConfiguration(Map<String, Object> config) throws ConfigurationException {
        // Example custom validation: if enabled, database.host must be provided
        String enabled = String.valueOf(config.getOrDefault("govaryn.example.enabled", "true"));
        if ("true".equalsIgnoreCase(enabled)) {
            String host = (String) config.get("govaryn.example.database.host");
            if (host == null || host.isBlank()) {
                throw new ConfigurationException("govaryn.example.database.host",
                    "must be provided when module is enabled");
            }
        }
    }
}

