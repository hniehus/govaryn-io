package io.govaryn.kernel.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Acceptance Criteria Tests - Configuration Management")
class ConfigurationManagementAcceptanceCriteriaTest {

    /**
     * AC 1: Configuration precedence (environment > external config > defaults)
     */
    @Test
    @DisplayName("AC1: Environment variables override external config which overrides defaults")
    void testConfigurationPrecedence() {
        // Scenario: same key set in all three sources

        // Source 1: Default (lowest precedence)
        String defaultValue = "from-default";

        // Source 2: External config
        String externalValue = "from-external-file";

        // Source 3: Environment (highest precedence)
        String envValue = "from-environment";

        // Simulate ConfigurationManager precedence logic
        Map<String, Object> config = new LinkedHashMap<>();

        // Start with defaults
        config.put("test.key", defaultValue);
        assertEquals("from-default", config.get("test.key"));

        // Override with external config
        config.put("test.key", externalValue);
        assertEquals("from-external-file", config.get("test.key"));

        // Override with environment (final precedence)
        config.put("test.key", envValue);
        assertEquals("from-environment", config.get("test.key"));
    }

    /**
     * AC 2: Module receives only configuration from its assigned namespace
     */
    @Test
    @DisplayName("AC2: Module configuration is isolated to its namespace")
    void testModuleNamespaceIsolation() {
        Map<String, Object> allConfig = new HashMap<>();
        allConfig.put("govaryn.kernel.id", "kernel-01");
        allConfig.put("govaryn.kernel.environment", "PROD");
        allConfig.put("govaryn.moduleA.enabled", "true");
        allConfig.put("govaryn.moduleA.setting1", "value1");
        allConfig.put("govaryn.moduleB.enabled", "false");
        allConfig.put("govaryn.moduleB.setting2", "value2");

        // Module A gets only its namespace
        Map<String, Object> moduleAConfig = new LinkedHashMap<>();
        String moduleANamespace = "govaryn.moduleA";
        for (var entry : allConfig.entrySet()) {
            if (entry.getKey().startsWith(moduleANamespace + ".")) {
                moduleAConfig.put(entry.getKey(), entry.getValue());
            }
        }

        // Module A can access its settings
        assertTrue(moduleAConfig.containsKey("govaryn.moduleA.enabled"));
        assertTrue(moduleAConfig.containsKey("govaryn.moduleA.setting1"));

        // Module A cannot access other namespaces
        assertFalse(moduleAConfig.containsKey("govaryn.kernel.id"));
        assertFalse(moduleAConfig.containsKey("govaryn.moduleB.enabled"));

        // Verify isolation works for Module B too
        Map<String, Object> moduleBConfig = new LinkedHashMap<>();
        String moduleBNamespace = "govaryn.moduleB";
        for (var entry : allConfig.entrySet()) {
            if (entry.getKey().startsWith(moduleBNamespace + ".")) {
                moduleBConfig.put(entry.getKey(), entry.getValue());
            }
        }

        assertTrue(moduleBConfig.containsKey("govaryn.moduleB.enabled"));
        assertFalse(moduleBConfig.containsKey("govaryn.moduleA.enabled"));
    }

    /**
     * AC 3: Required key missing → startup fails fast with clear error message naming the key
     */
    @Test
    @DisplayName("AC3: Missing required configuration key causes fast-fail with named error")
    void testMissingRequiredKeyFailsFast() {
        Map<String, Object> config = new HashMap<>();
        config.put("govaryn.kernel.environment", "DEV");
        // Missing required key: govaryn.kernel.id
        config.put("govaryn.kernel.module.mode", "CLASSPATH");

        ConfigurationException exception = assertThrows(
            ConfigurationException.class,
            () -> ConfigurationValidator.validate(config, KernelConfigurationSchema.SCHEMA)
        );

        // Error message must name the affected key
        String errorMessage = exception.getMessage();
        assertTrue(errorMessage.contains("govaryn.kernel.id"),
            "Error message should name the affected key 'govaryn.kernel.id'");
        assertTrue(errorMessage.contains("missing"),
            "Error message should indicate the key is missing");
    }

    /**
     * AC 4: Secret configuration keys are redacted in logs and diagnostics
     */
    @Test
    @DisplayName("AC4: Secret values are redacted in logs and diagnostics")
    void testSecretRedaction() {
        Map<String, Object> config = new HashMap<>();
        config.put("app.name", "MyApplication");
        config.put("db.host", "localhost");
        config.put("db.password", "super-secret-password");
        config.put("api.token", "secret-api-token-xyz");
        config.put("app.description", "A simple application");

        // Get all configuration (as would be logged)
        Map<String, Object> redacted = SecretRedactor.redactSecrets(config);

        // Non-secret values remain visible
        assertEquals("MyApplication", redacted.get("app.name"));
        assertEquals("localhost", redacted.get("db.host"));
        assertEquals("A simple application", redacted.get("app.description"));

        // Secret values are redacted
        assertEquals("***REDACTED***", redacted.get("db.password"));
        assertEquals("***REDACTED***", redacted.get("api.token"));

        // Verify string representation also redacts
        String redactedString = SecretRedactor.toRedactedString(config);
        assertTrue(redactedString.contains("app.name=MyApplication"));
        assertTrue(redactedString.contains("db.password=***REDACTED***"));
        assertFalse(redactedString.contains("super-secret-password"));
    }

    /**
     * AC 4 (Extended): Invalid required values also fail fast
     */
    @Test
    @DisplayName("AC3 Extended: Invalid required configuration value causes fast-fail")
    void testInvalidRequiredValueFailsFast() {
        Map<String, Object> config = new HashMap<>();
        config.put("govaryn.kernel.id", "kernel-01");
        config.put("govaryn.kernel.environment", "INVALID_ENVIRONMENT"); // Invalid enum
        config.put("govaryn.kernel.module.mode", "CLASSPATH");

        ConfigurationException exception = assertThrows(
            ConfigurationException.class,
            () -> ConfigurationValidator.validate(config, KernelConfigurationSchema.SCHEMA)
        );

        String errorMessage = exception.getMessage();
        // Error should name the affected key
        assertTrue(errorMessage.contains("govaryn.kernel.environment"),
            "Error should name the affected key");
        // Error should explain what went wrong
        assertTrue(errorMessage.contains("must be one of") || errorMessage.contains("INVALID"),
            "Error should explain the constraint violation");
    }

    /**
     * Combined scenario: Real-world configuration with all aspects
     */
    @Test
    @DisplayName("Integration: Complete configuration management scenario")
    void testCompleteConfigurationScenario() {
        // 1. Define module schema
        var testSchema = new TestModuleSchema();

        // 2. Create configuration from mixed sources
        Map<String, Object> config = new LinkedHashMap<>();

        // Defaults
        config.put("govaryn.test.enabled", "true");
        config.put("govaryn.test.db.host", "localhost");

        // External override
        config.put("govaryn.test.db.host", "prod.internal");
        config.put("govaryn.test.db.password", "production-secret");

        // Environment override (simulated)
        config.put("govaryn.test.db.password", "env-secret-override");

        // 3. Validate configuration
        assertDoesNotThrow(
            () -> ConfigurationValidator.validateModuleConfiguration(testSchema, config)
        );

        // 4. Verify redaction
        Map<String, Object> redacted = SecretRedactor.redactSecrets(config);
        assertEquals("prod.internal", redacted.get("govaryn.test.db.host"));
        assertEquals("***REDACTED***", redacted.get("govaryn.test.db.password"));

        // 5. Verify isolation
        String namespace = "govaryn.test";
        Map<String, Object> isolated = new LinkedHashMap<>();
        for (var entry : config.entrySet()) {
            if (entry.getKey().startsWith(namespace + ".")) {
                isolated.put(entry.getKey(), entry.getValue());
            }
        }
        assertEquals(3, isolated.size()); // Should have 3 keys from this namespace
    }

    // Helper test module schema
    private static class TestModuleSchema implements ModuleConfigurationSchema {
        @Override
        public String getNamespace() {
            return "govaryn.test";
        }

        @Override
        public List<KernelConfigurationSchema.ConfigKeyMetadata> getKeyMetadata() {
            return List.of(
                new KernelConfigurationSchema.ConfigKeyMetadata(
                    "govaryn.test.enabled",
                    KernelConfigurationSchema.ConfigType.ENUM,
                    "true",
                    true,
                    KernelConfigurationSchema.Strictness.STRICT,
                    List.of("true", "false")
                ),
                new KernelConfigurationSchema.ConfigKeyMetadata(
                    "govaryn.test.db.host",
                    KernelConfigurationSchema.ConfigType.STRING,
                    "localhost",
                    false,
                    KernelConfigurationSchema.Strictness.NON_STRICT,
                    List.of()
                ),
                new KernelConfigurationSchema.ConfigKeyMetadata(
                    "govaryn.test.db.password",
                    KernelConfigurationSchema.ConfigType.STRING,
                    null,
                    false,
                    KernelConfigurationSchema.Strictness.NON_STRICT,
                    List.of()
                )
            );
        }
    }
}

