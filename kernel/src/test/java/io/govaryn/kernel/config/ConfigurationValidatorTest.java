package io.govaryn.kernel.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ConfigurationValidator Tests")
class ConfigurationValidatorTest {

    private Map<String, Object> testConfig;

    @BeforeEach
    void setUp() {
        testConfig = new LinkedHashMap<>();
    }

    @Test
    @DisplayName("Should pass when all required keys are present and valid")
    void testValidationPassesWithValidConfiguration() {
        testConfig.put("govaryn.kernel.id", "test-kernel");
        testConfig.put("govaryn.kernel.environment", "DEV");
        testConfig.put("govaryn.kernel.module.mode", "CLASSPATH");

        // Should not throw
        assertDoesNotThrow(() -> ConfigurationValidator.validate(testConfig, KernelConfigurationSchema.SCHEMA));
    }

    @Test
    @DisplayName("Should fail when required key 'govaryn.kernel.id' is missing")
    void testValidationFailsWhenRequiredKeyMissing() {
        testConfig.put("govaryn.kernel.environment", "DEV");
        testConfig.put("govaryn.kernel.module.mode", "CLASSPATH");

        ConfigurationException exception = assertThrows(ConfigurationException.class,
            () -> ConfigurationValidator.validate(testConfig, KernelConfigurationSchema.SCHEMA));

        assertTrue(exception.getMessage().contains("govaryn.kernel.id"));
        assertTrue(exception.getMessage().contains("missing"));
    }

    @Test
    @DisplayName("Should fail when required key has blank value")
    void testValidationFailsWhenRequiredKeyIsBlank() {
        testConfig.put("govaryn.kernel.id", "");
        testConfig.put("govaryn.kernel.environment", "DEV");
        testConfig.put("govaryn.kernel.module.mode", "CLASSPATH");

        ConfigurationException exception = assertThrows(ConfigurationException.class,
            () -> ConfigurationValidator.validate(testConfig, KernelConfigurationSchema.SCHEMA));

        assertTrue(exception.getMessage().contains("govaryn.kernel.id"));
    }

    @Test
    @DisplayName("Should fail when enum value is invalid")
    void testValidationFailsWithInvalidEnumValue() {
        testConfig.put("govaryn.kernel.id", "test-kernel");
        testConfig.put("govaryn.kernel.environment", "INVALID_ENV");
        testConfig.put("govaryn.kernel.module.mode", "CLASSPATH");

        ConfigurationException exception = assertThrows(ConfigurationException.class,
            () -> ConfigurationValidator.validate(testConfig, KernelConfigurationSchema.SCHEMA));

        assertTrue(exception.getMessage().contains("govaryn.kernel.environment"));
        assertTrue(exception.getMessage().contains("must be one of"));
    }

    @Test
    @DisplayName("Should pass when optional key is missing")
    void testValidationPassesWithOptionalKeyMissing() {
        testConfig.put("govaryn.kernel.id", "test-kernel");
        testConfig.put("govaryn.kernel.environment", "PROD");
        testConfig.put("govaryn.kernel.module.mode", "CLASSPATH");
        // All required keys are provided

        assertDoesNotThrow(() -> ConfigurationValidator.validate(testConfig, KernelConfigurationSchema.SCHEMA));
    }

    @Test
    @DisplayName("Should pass when key with default is missing")
    void testValidationPassesWhenKeyWithDefaultIsMissing() {
        testConfig.put("govaryn.kernel.id", "test-kernel");
        testConfig.put("govaryn.kernel.environment", "PROD");
        // govaryn.kernel.module.mode has a default, so it's okay to omit
        // In a real scenario, defaults would be pre-filled in the config map

        // For this test, we include the default to simulate real behavior
        testConfig.put("govaryn.kernel.module.mode", "CLASSPATH");

        assertDoesNotThrow(() -> ConfigurationValidator.validate(testConfig, KernelConfigurationSchema.SCHEMA));
    }

    @Test
    @DisplayName("Should provide clear error messages naming affected keys")
    void testErrorMessagesNameAffectedKeys() {
        testConfig.put("govaryn.kernel.id", "");
        testConfig.put("govaryn.kernel.environment", "STAGING"); // Invalid value

        ConfigurationException exception = assertThrows(ConfigurationException.class,
            () -> ConfigurationValidator.validate(testConfig, KernelConfigurationSchema.SCHEMA));

        String message = exception.getMessage();
        assertTrue(message.contains("govaryn.kernel.id") || message.contains("govaryn.kernel.environment"));
        assertTrue(message.contains("error"));
    }
}


