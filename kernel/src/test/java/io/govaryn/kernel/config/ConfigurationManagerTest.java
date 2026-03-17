package io.govaryn.kernel.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ConfigurationManager Tests")
class ConfigurationManagerTest {

    private static class TestModuleSchema implements ModuleConfigurationSchema {

        @Override
        public String getNamespace() {
            return "govaryn.testmodule";
        }

        @Override
        public List<KernelConfigurationSchema.ConfigKeyMetadata> getKeyMetadata() {
            return List.of(
                new KernelConfigurationSchema.ConfigKeyMetadata(
                    "govaryn.testmodule.enabled",
                    KernelConfigurationSchema.ConfigType.ENUM,
                    "true",
                    true,
                    KernelConfigurationSchema.Strictness.STRICT,
                    List.of("true", "false")
                ),
                new KernelConfigurationSchema.ConfigKeyMetadata(
                    "govaryn.testmodule.api.key",
                    KernelConfigurationSchema.ConfigType.STRING,
                    null,
                    false,
                    KernelConfigurationSchema.Strictness.NON_STRICT,
                    List.of()
                )
            );
        }
    }

    @Test
    @DisplayName("Should return namespaced configuration for a module")
    void testGetNamespacedConfiguration() {
        var config = new LinkedHashMap<String, Object>();
        config.put("govaryn.kernel.id", "test");
        config.put("govaryn.testmodule.enabled", "true");
        config.put("govaryn.testmodule.api.key", "secret-key");
        config.put("other.setting", "value");

        ConfigurationManager manager = new ConfigurationManager(null) {
            {
                this.mergedConfiguration = config;
            }
        };

        Map<String, Object> namespaced = manager.getNamespacedConfiguration("govaryn.testmodule");

        assertEquals(2, namespaced.size());
        assertTrue(namespaced.containsKey("govaryn.testmodule.enabled"));
        assertTrue(namespaced.containsKey("govaryn.testmodule.api.key"));
        assertFalse(namespaced.containsKey("govaryn.kernel.id"));
        assertFalse(namespaced.containsKey("other.setting"));
    }

    @Test
    @DisplayName("Should get individual configuration values")
    void testGetConfigurationValue() {
        var config = new LinkedHashMap<String, Object>();
        config.put("govaryn.kernel.id", "test-kernel");
        config.put("govaryn.kernel.environment", "DEV");

        ConfigurationManager manager = new ConfigurationManager(null) {
            {
                this.mergedConfiguration = config;
            }
        };

        assertEquals("test-kernel", manager.get("govaryn.kernel.id"));
        assertEquals("DEV", manager.get("govaryn.kernel.environment"));
        assertNull(manager.get("nonexistent.key"));
    }

    @Test
    @DisplayName("Should get configuration value as string")
    void testGetStringValue() {
        var config = new LinkedHashMap<String, Object>();
        config.put("govaryn.kernel.id", "test-kernel");

        ConfigurationManager manager = new ConfigurationManager(null) {
            {
                this.mergedConfiguration = config;
            }
        };

        String value = manager.getString("govaryn.kernel.id");
        assertEquals("test-kernel", value);
    }

    @Test
    @DisplayName("Should redact secrets in getAll()")
    void testSecretsRedactedInGetAll() {
        var config = new LinkedHashMap<String, Object>();
        config.put("app.name", "TestApp");
        config.put("db.password", "secret123");

        ConfigurationManager manager = new ConfigurationManager(null) {
            {
                this.mergedConfiguration = config;
            }
        };

        Map<String, Object> all = manager.getAll();
        assertEquals("TestApp", all.get("app.name"));
        assertEquals("***REDACTED***", all.get("db.password"));
    }

    @Test
    @DisplayName("Should provide unredacted configuration with getAllUnredacted()")
    void testGetAllUnredacted() {
        var config = new LinkedHashMap<String, Object>();
        config.put("app.name", "TestApp");
        config.put("db.password", "secret123");

        ConfigurationManager manager = new ConfigurationManager(null) {
            {
                this.mergedConfiguration = config;
            }
        };

        Map<String, Object> all = manager.getAllUnredacted();
        assertEquals("secret123", all.get("db.password"));
    }

    @Test
    @DisplayName("Should check if keys are allowed")
    void testIsKeyAllowed() {
        ConfigurationManager manager = new ConfigurationManager(null);
        manager.registerModule(new TestModuleSchema());

        assertTrue(manager.isKeyAllowed("govaryn.kernel.id"));
        assertTrue(manager.isKeyAllowed("govaryn.testmodule.enabled"));
        assertFalse(manager.isKeyAllowed("undefined.key"));
    }

    @Test
    @DisplayName("Should provide status summary")
    void testGetStatusSummary() {
        var config = new LinkedHashMap<String, Object>();
        config.put("govaryn.kernel.id", "test");

        ConfigurationManager manager = new ConfigurationManager(null) {
            {
                this.mergedConfiguration = config;
            }
        };

        manager.registerModule(new TestModuleSchema());

        String summary = manager.getStatusSummary();
        assertTrue(summary.contains("ConfigurationManager"));
        assertTrue(summary.contains("keys=1"));
        assertTrue(summary.contains("registeredModules=1"));
    }
}

