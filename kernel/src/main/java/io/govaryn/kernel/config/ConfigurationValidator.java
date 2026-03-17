package io.govaryn.kernel.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Validates configuration during startup.
 * Checks for required keys, invalid values, and schema violations.
 * Fails fast with clear error messages naming the affected keys.
 */
public class ConfigurationValidator {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ConfigurationValidator.class);

    /**
     * Validate configuration values against a schema and precedence rules.
     * Throws ConfigurationException if validation fails.
     *
     * @param configuration Merged configuration (after precedence applied)
     * @param schema Configuration schema
     * @throws ConfigurationException if validation fails
     */
    public static void validate(Map<String, Object> configuration, Map<String, KernelConfigurationSchema.ConfigKeyMetadata> schema) {
        List<String> errors = new ArrayList<>();

        for (var entry : schema.entrySet()) {
            String key = entry.getKey();
            KernelConfigurationSchema.ConfigKeyMetadata metadata = entry.getValue();

            Object value = configuration.get(key);

            // Check if required
            if (metadata.required() && value == null) {
                errors.add(String.format("Required configuration key '%s' is missing", key));
                continue;
            }

            // Skip further validation if optional and not provided
            if (value == null && !metadata.required()) {
                continue;
            }

            // Validate value type and content
            try {
                validateKeyValue(key, value, metadata);
            } catch (ConfigurationException e) {
                errors.add(e.getMessage());
            }
        }

        if (!errors.isEmpty()) {
            String message = "Configuration validation failed with " + errors.size() + " error(s):\n" +
                    String.join("\n", errors);
            logger.error(message);
            throw new ConfigurationException(message);
        }

        logger.info("Configuration validation passed");
    }

    /**
     * Validate a single configuration key-value pair against its metadata.
     *
     * @param key Configuration key
     * @param value Configuration value
     * @param metadata Key metadata
     * @throws ConfigurationException if validation fails
     */
    private static void validateKeyValue(String key, Object value, KernelConfigurationSchema.ConfigKeyMetadata metadata) {
        if (value == null && metadata.required()) {
            throw new ConfigurationException(key, "is required but not provided");
        }

        if (value == null) {
            return; // Optional key with no value
        }

        // Type validation
        switch (metadata.type()) {
            case STRING:
                if (!(value instanceof String)) {
                    throw new ConfigurationException(key, String.format("must be a string, got %s", value.getClass().getSimpleName()));
                }
                String strValue = (String) value;
                if (strValue.isBlank()) {
                    throw new ConfigurationException(key, "must not be blank");
                }
                break;

            case ENUM:
                String enumValue = String.valueOf(value);
                if (metadata.strictness() == KernelConfigurationSchema.Strictness.STRICT) {
                    if (!metadata.allowedValues().contains(enumValue)) {
                        throw new ConfigurationException(key, String.format(
                            "must be one of %s, got '%s'", metadata.allowedValues(), enumValue));
                    }
                }
                break;

            default:
                throw new ConfigurationException(key, "Unknown type: " + metadata.type());
        }
    }

    /**
     * Validate module configuration against a module's schema.
     *
     * @param moduleSchema The module's schema
     * @param configuration Configuration values for the module's namespace
     * @throws ConfigurationException if validation fails
     */
    public static void validateModuleConfiguration(ModuleConfigurationSchema moduleSchema, Map<String, Object> configuration) {
        String namespace = moduleSchema.getNamespace();
        List<String> errors = new ArrayList<>();

        for (var metadata : moduleSchema.getKeyMetadata()) {
            String key = metadata.name();

            // Ensure key belongs to the module's namespace
            if (!key.startsWith(namespace + ".")) {
                errors.add(String.format("Configuration key '%s' does not belong to namespace '%s'", key, namespace));
                continue;
            }

            Object value = configuration.get(key);

            if (metadata.required() && value == null) {
                errors.add(String.format("Required key '%s' in module '%s' is missing", key, namespace));
                continue;
            }

            if (value == null && !metadata.required()) {
                continue;
            }

            try {
                validateKeyValue(key, value, metadata);
            } catch (ConfigurationException e) {
                errors.add(e.getMessage());
            }
        }

        if (!errors.isEmpty()) {
            String message = String.format("Configuration validation failed for module '%s' with %d error(s):\n%s",
                    namespace, errors.size(), String.join("\n", errors));
            logger.error(message);
            throw new ConfigurationException(message);
        }

        // Call module's custom validation
        try {
            moduleSchema.validateConfiguration(configuration);
        } catch (ConfigurationException e) {
            throw e;
        } catch (Exception e) {
            throw new ConfigurationException("Custom validation failed for module: " + namespace, e.getMessage(), e);
        }

        logger.info("Configuration validation passed for module '{}'", namespace);
    }
}

