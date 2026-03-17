package io.govaryn.kernel.config;

/**
 * Exception thrown when configuration validation fails.
 */
public class ConfigurationException extends RuntimeException {

    private final String configKey;

    public ConfigurationException(String message) {
        super(message);
        this.configKey = null;
    }

    public ConfigurationException(String configKey, String message) {
        super(String.format("Configuration error for key '%s': %s", configKey, message));
        this.configKey = configKey;
    }

    public ConfigurationException(String configKey, String message, Throwable cause) {
        super(String.format("Configuration error for key '%s': %s", configKey, message), cause);
        this.configKey = configKey;
    }

    public String getConfigKey() {
        return configKey;
    }
}

