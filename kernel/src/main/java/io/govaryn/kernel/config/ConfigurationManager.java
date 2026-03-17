package io.govaryn.kernel.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.*;

import static io.govaryn.kernel.config.SecretRedactor.*;

/**
 * Centralized configuration manager for the Govaryn Kernel.
 *
 * Merges configuration from multiple sources with a defined precedence:
 * 1. Environment variables (highest precedence)
 * 2. External configuration files
 * 3. Application defaults (lowest precedence)
 *
 * Provides configuration to modules in their assigned namespaces.
 * Validates configuration on startup and redacts secrets in logs.
 */
@Component
public class ConfigurationManager {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationManager.class);

    private final Environment environment;
    static Map<String, Object> mergedConfiguration;
    private Map<String, ModuleConfigurationSchema> registeredModules = new HashMap<>();

    @Autowired
    public ConfigurationManager(Environment environment) {
        this.environment = environment;
    }

    /**
     * Initialize the configuration manager.
     * Loads and merges configuration from all sources, validates it, and logs setup.
     * Should be called early in application startup.
     *
     * @throws ConfigurationException if validation fails
     */
    public void initialize() {
        logger.info("Initializing ConfigurationManager");

        this.mergedConfiguration = loadAndMergeConfiguration();

        // Validate kernel configuration
        ConfigurationValidator.validate(mergedConfiguration, KernelConfigurationSchema.SCHEMA);

        // Validate module configurations
        for (var moduleSchema : registeredModules.values()) {
            Map<String, Object> moduleConfig = getNamespacedConfiguration(moduleSchema.getNamespace());
            ConfigurationValidator.validateModuleConfiguration(moduleSchema, moduleConfig);
        }

        logger.info("ConfigurationManager initialized successfully");
        if (logger.isDebugEnabled()) {
            logger.debug("Merged configuration: {}", toRedactedString(mergedConfiguration));
        }
    }

    /**
     * Register a module's configuration schema.
     * Must be called before initialize() to validate the module's configuration.
     *
     * @param schema The module's configuration schema
     */
    public void registerModule(ModuleConfigurationSchema schema) {
        String namespace = schema.getNamespace();
        logger.info("Registering configuration schema for module: {}", namespace);
        registeredModules.put(namespace, schema);
    }

    /**
     * Load and merge configuration from all sources with precedence:
     * Environment > External Config > Defaults
     *
     * @return Merged configuration map
     */
    private Map<String, Object> loadAndMergeConfiguration() {
        Map<String, Object> defaults = loadDefaults();
        Map<String, Object> externalConfig = loadExternalConfiguration();
        Map<String, Object> envConfig = loadEnvironmentConfiguration();

        Map<String, Object> merged = new LinkedHashMap<>();

        // Start with defaults
        merged.putAll(defaults);
        if (logger.isDebugEnabled()) {
            logger.debug("Loaded defaults: {}", toRedactedString(defaults));
        }

        // Override with external config
        merged.putAll(externalConfig);
        if (logger.isDebugEnabled()) {
            logger.debug("Applied external configuration: {}", toRedactedString(externalConfig));
        }

        // Override with environment variables (highest precedence)
        merged.putAll(envConfig);
        if (logger.isDebugEnabled()) {
            logger.debug("Applied environment configuration: {}", toRedactedString(envConfig));
        }

        return merged;
    }

    /**
     * Load configuration from defaults.
     * In a real implementation, this could load from a resource file or hardcoded values.
     *
     * @return Default configuration
     */
    private Map<String, Object> loadDefaults() {
        Map<String, Object> defaults = new LinkedHashMap<>();

        // Apply default values from schema
        for (var entry : KernelConfigurationSchema.SCHEMA.entrySet()) {
            if (entry.getValue().defaultValue() != null) {
                defaults.put(entry.getKey(), entry.getValue().defaultValue());
            }
        }

        // Include module defaults
        for (var moduleSchema : registeredModules.values()) {
            for (var metadata : moduleSchema.getKeyMetadata()) {
                if (metadata.defaultValue() != null) {
                    defaults.put(metadata.name(), metadata.defaultValue());
                }
            }
        }

        return defaults;
    }

    /**
     * Load configuration from external sources (application.properties, YAML files, etc.).
     * Uses Spring's Environment to read properties.
     *
     * @return External configuration
     */
    private Map<String, Object> loadExternalConfiguration() {
        Map<String, Object> externalConfig = new LinkedHashMap<>();

        // Collect all keys from schema and module schemas
        Set<String> keysToLoad = new HashSet<>(KernelConfigurationSchema.SCHEMA.keySet());
        for (var moduleSchema : registeredModules.values()) {
            for (var metadata : moduleSchema.getKeyMetadata()) {
                keysToLoad.add(metadata.name());
            }
        }

        // Load each key from Spring's Environment (which reads from application.properties, application.yml, etc.)
        for (String key : keysToLoad) {
            String value = environment.getProperty(key);
            if (value != null) {
                externalConfig.put(key, value);
            }
        }

        return externalConfig;
    }

    /**
     * Load configuration from environment variables.
     * Converts environment variable names to configuration key names.
     * Example: GOVARYN_KERNEL_ID -> govaryn.kernel.id
     *
     * @return Environment variable configuration
     */
    private Map<String, Object> loadEnvironmentConfiguration() {
        Map<String, Object> envConfig = new LinkedHashMap<>();

        Set<String> keysToLoad = new HashSet<>(KernelConfigurationSchema.SCHEMA.keySet());
        for (var moduleSchema : registeredModules.values()) {
            for (var metadata : moduleSchema.getKeyMetadata()) {
                keysToLoad.add(metadata.name());
            }
        }

        // Check environment variables with converted names
        for (String key : keysToLoad) {
            String envVarName = convertKeyToEnvVarName(key);
            String value = System.getenv(envVarName);
            if (value != null) {
                envConfig.put(key, value);
            }
        }

        return envConfig;
    }

    /**
     * Convert configuration key to environment variable name.
     * Example: govaryn.kernel.id -> GOVARYN_KERNEL_ID
     *
     * @param key Configuration key
     * @return Environment variable name
     */
    private String convertKeyToEnvVarName(String key) {
        return key.toUpperCase().replace(".", "_");
    }

    /**
     * Get all configuration values for a specific module namespace.
     * Returns only keys that belong to the namespace.
     *
     * @param namespace Module namespace (e.g., "govaryn.mymodule")
     * @return Configuration values for the namespace
     */
    public Map<String, Object> getNamespacedConfiguration(String namespace) {
        Map<String, Object> namespaced = new LinkedHashMap<>();

        String prefix = namespace + ".";
        for (var entry : mergedConfiguration.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                namespaced.put(entry.getKey(), entry.getValue());
            }
        }

        return namespaced;
    }

    /**
     * Get a single configuration value by key.
     *
     * @param key Configuration key
     * @return Configuration value, or null if not found
     */
    public Object get(String key) {
        return mergedConfiguration.get(key);
    }

    /**
     * Get a configuration value as a String.
     *
     * @param key Configuration key
     * @return Configuration value as String, or null if not found
     */
    public String getString(String key) {
        Object value = mergedConfiguration.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * Get all merged configuration values.
     *
     * @return All configuration (with secrets redacted for safety)
     */
    public Map<String, Object> getAll() {
        return redactSecrets(new LinkedHashMap<>(mergedConfiguration));
    }

    /**
     * Get all configuration values without redaction.
     * Use with caution as this includes secrets!
     *
     * @return All unredacted configuration
     */
    public Map<String, Object> getAllUnredacted() {
        return new LinkedHashMap<>(mergedConfiguration);
    }

    /**
     * Check if a configuration key is registered in any schema.
     *
     * @param key Configuration key
     * @return true if the key is allowed
     */
    public boolean isKeyAllowed(String key) {
        if (KernelConfigurationSchema.isKeyAllowed(key)) {
            return true;
        }

        for (var moduleSchema : registeredModules.values()) {
            for (var metadata : moduleSchema.getKeyMetadata()) {
                if (metadata.name().equals(key)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Get a summary of configuration status for diagnostics.
     *
     * @return Summary string
     */
    public String getStatusSummary() {
        return String.format(
            "ConfigurationManager[keys=%d, registeredModules=%d]",
            mergedConfiguration.size(),
            registeredModules.size()
        );
    }
}

