package io.govaryn.kernel.config;

import java.util.List;
import java.util.Map;

/**
 * Central schema for all Govaryn Kernel configuration settings.
 * Defines metadata for each key: name, type, default value, requirement, and strictness.
 * This serves as the canonical reference for validation and documentation.
 */
public class KernelConfigurationSchema {

    // Enum for types
    public enum ConfigType {
        STRING, ENUM
    }

    // Enum for strictness (strict: only defined values allowed; non-strict: any values)
    public enum Strictness {
        STRICT, NON_STRICT
    }

    // Record for metadata of a configuration key
    public record ConfigKeyMetadata(
        String name,              // Canonical name (e.g., "govaryn.kernel.id")
        ConfigType type,          // Data type
        Object defaultValue,      // Default value (null if none)
        boolean required,         // Required (true) or optional (false)
        Strictness strictness,    // Strictness of validation
        List<String> allowedValues // Allowed values (for enums or strict strings)
    ) {}

    // Central map with all defined keys
    public static final Map<String, ConfigKeyMetadata> SCHEMA = Map.of(
        "govaryn.kernel.id", new ConfigKeyMetadata(
            "govaryn.kernel.id",
            ConfigType.STRING,
            null, // No default value
            true, // Required
            Strictness.NON_STRICT, // Any non-empty strings allowed
            List.of() // No specific allowed values
        ),
        "govaryn.kernel.environment", new ConfigKeyMetadata(
            "govaryn.kernel.environment",
            ConfigType.ENUM,
            null, // No default value (must be set)
            true, // Required
            Strictness.STRICT, // Only defined enum values
            List.of("DEV", "STAGE", "PROD")
        ),
        "govaryn.kernel.module.mode", new ConfigKeyMetadata(
            "govaryn.kernel.module.mode",
            ConfigType.ENUM,
            "CLASSPATH", // Default value
            true, // Required (but with default)
            Strictness.STRICT, // Only defined enum values
            List.of("CLASSPATH", "PLUGIN_FOLDER")
        )
    );

    /**
     * Validates if a key is defined in the schema.
     * @param key The canonical key
     * @return true if allowed; false if invalid
     */
    public static boolean isKeyAllowed(String key) {
        return SCHEMA.containsKey(key);
    }

    /**
     * Returns metadata for a key.
     * @param key The canonical key
     * @return Metadata or null if not defined
     */
    public static ConfigKeyMetadata getMetadata(String key) {
        return SCHEMA.get(key);
    }

    /**
     * Returns a list of all allowed keys.
     * @return List of canonical names
     */
    public static List<String> getAllowedKeys() {
        return List.copyOf(SCHEMA.keySet());
    }
}
