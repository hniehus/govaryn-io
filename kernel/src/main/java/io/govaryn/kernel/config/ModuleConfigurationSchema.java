package io.govaryn.kernel.config;

import java.util.List;

/**
 * Schema declaration for a module's configuration namespace.
 * Modules implement this interface to declare their configuration requirements and metadata.
 */
public interface ModuleConfigurationSchema {

    /**
     * Returns the namespace prefix for this module's configuration.
     * Example: "govaryn.mymodule" means all config keys start with this prefix.
     *
     * @return The namespace prefix (e.g., "govaryn.mymodule")
     */
    String getNamespace();

    /**
     * Returns the list of configuration key metadata for this module.
     * Each metadata defines the key name, type, default value, requirement, strictness, and allowed values.
     *
     * @return List of ConfigKeyMetadata objects
     */
    List<KernelConfigurationSchema.ConfigKeyMetadata> getKeyMetadata();

    /**
     * Optional: Validate configuration values after they are loaded.
     * Throw ConfigurationException if validation fails.
     *
     * @param config Configuration values keyed by property names
     * @throws ConfigurationException if validation fails
     */
    default void validateConfiguration(java.util.Map<String, Object> config) throws ConfigurationException {
        // Default: no additional validation
    }
}

