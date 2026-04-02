# Configuration Management Integration Guide

This guide helps module developers integrate with the Govaryn Kernel's centralized configuration management system.

## Quick Start

### 1. Define Your Module's Configuration Schema

Create a class implementing `ModuleConfigurationSchema`:

```java
package io.govaryn.mymodule.config;

import io.govaryn.kernel.config.*;
import java.util.List;
import java.util.Map;

public class MyModuleConfigurationSchema implements ModuleConfigurationSchema {

    @Override
    public String getNamespace() {
        return "govaryn.mymodule";
    }

    @Override
    public List<KernelConfigurationSchema.ConfigKeyMetadata> getKeyMetadata() {
        return List.of(
            // Required configuration
            new KernelConfigurationSchema.ConfigKeyMetadata(
                "govaryn.mymodule.feature.enabled",
                KernelConfigurationSchema.ConfigType.ENUM,
                "true",  // Default value
                true,    // Required
                KernelConfigurationSchema.Strictness.STRICT,
                List.of("true", "false")
            ),
            
            // Optional configuration
            new KernelConfigurationSchema.ConfigKeyMetadata(
                "govaryn.mymodule.database.url",
                KernelConfigurationSchema.ConfigType.STRING,
                null,  // No default
                false, // Optional
                KernelConfigurationSchema.Strictness.NON_STRICT,
                List.of()
            ),
            
            // Sensitive configuration (password detected automatically)
            new KernelConfigurationSchema.ConfigKeyMetadata(
                "govaryn.mymodule.database.password",
                KernelConfigurationSchema.ConfigType.STRING,
                null,
                false,
                KernelConfigurationSchema.Strictness.NON_STRICT,
                List.of()
                // Key name contains "password" → auto-redacted
            )
        );
    }

    @Override
    public void validateConfiguration(Map<String, Object> config) throws ConfigurationException {
        // Optional: custom validation logic
        
        // Example: cross-field validation
        boolean enabled = "true".equalsIgnoreCase(
            String.valueOf(config.getOrDefault("govaryn.mymodule.feature.enabled", "true"))
        );
        
        if (enabled) {
            String dbUrl = (String) config.get("govaryn.mymodule.database.url");
            if (dbUrl == null || dbUrl.isBlank()) {
                throw new ConfigurationException(
                    "govaryn.mymodule.database.url",
                    "must be set when feature is enabled"
                );
            }
        }
    }
}
```

### 2. Register Your Schema with the Kernel

Create a component that registers your schema during application startup:

```java
package io.govaryn.mymodule.config;

import io.govaryn.kernel.config.ConfigurationManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MyModuleConfigurationInitializer {
    
    @Autowired
    public MyModuleConfigurationInitializer(ConfigurationManager configManager) {
        // Register the schema before ConfigurationManager.initialize() is called
        configManager.registerModule(new MyModuleConfigurationSchema());
    }
}
```

**Important:** Registration must happen early, ideally in a component that's created before the kernel finishes startup.

### 3. Document Your Configuration in application.properties

Create an example configuration file for your module:

```properties
# MyModule Configuration

# Feature flag: enable/disable the module
govaryn.mymodule.feature.enabled=true

# Database connection
govaryn.mymodule.database.url=jdbc:postgresql://localhost:5432/mydb
govaryn.mymodule.database.password=your-database-password
```

### 4. Access Configuration in Your Code

Use the `ConfigurationManager` to retrieve configuration:

```java
package io.govaryn.mymodule.service;

import io.govaryn.kernel.config.ConfigurationManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class MyModuleService {
    
    private final ConfigurationManager configManager;
    private final Map<String, Object> moduleConfig;

    @Autowired
    public MyModuleService(ConfigurationManager configManager) {
        this.configManager = configManager;
        // Cache module configuration at startup
        this.moduleConfig = configManager.getNamespacedConfiguration("govaryn.mymodule");
    }

    public void doSomething() {
        // Get string value
        String dbUrl = configManager.getString("govaryn.mymodule.database.url");

        // Get as object and cast
        String enabled = (String) moduleConfig.get("govaryn.mymodule.feature.enabled");

        // Work with values
        if ("true".equalsIgnoreCase(enabled)) {
            connectToDatabase(dbUrl);
        }
    }
}
```

### 5. Use Configuration in Module Initialization

Modules can also access configuration through `KernelContext`:

```java
package io.govaryn.mymodule;

import io.govaryn.kernel.api.KernelModule;
import io.govaryn.kernel.api.KernelContext;
import io.govaryn.kernel.config.ConfigurationManager;
import java.util.Map;

public class MyModule implements KernelModule {
    
    @Override
    public void init(KernelContext context) {
        ConfigurationManager config = context.configurationManager();
        Map<String, Object> myConfig = config.getNamespacedConfiguration("govaryn.mymodule");
        
        // Initialize module with configuration
        String dbUrl = (String) myConfig.get("govaryn.mymodule.database.url");
        System.out.println("MyModule initialized with database: " + dbUrl);
    }

    @Override
    public void start() {
        // Module startup logic
    }
}
```

## Configuration Examples

### Example 1: Simple Feature Toggle

```properties
# application.properties
govaryn.mymodule.feature.enabled=true
```

Schema definition:
```java
new KernelConfigurationSchema.ConfigKeyMetadata(
    "govaryn.mymodule.feature.enabled",
    KernelConfigurationSchema.ConfigType.ENUM,
    "false",  // Default: disabled
    true,     // Required
    KernelConfigurationSchema.Strictness.STRICT,
    List.of("true", "false")
)
```

### Example 2: Optional Database Connection

```properties
# application.properties
govaryn.mymodule.database.host=postgres.internal
govaryn.mymodule.database.port=5432
govaryn.mymodule.database.name=mydb
govaryn.mymodule.database.username=dbuser
govaryn.mymodule.database.password=secret-password
```

Schema definition:
```java
List.of(
    new KernelConfigurationSchema.ConfigKeyMetadata(
        "govaryn.mymodule.database.host",
        KernelConfigurationSchema.ConfigType.STRING,
        "localhost",
        false,  // Optional with default
        KernelConfigurationSchema.Strictness.NON_STRICT,
        List.of()
    ),
    new KernelConfigurationSchema.ConfigKeyMetadata(
        "govaryn.mymodule.database.port",
        KernelConfigurationSchema.ConfigType.STRING,
        "5432",
        false,
        KernelConfigurationSchema.Strictness.NON_STRICT,
        List.of()
    ),
    // ... username ...
    new KernelConfigurationSchema.ConfigKeyMetadata(
        "govaryn.mymodule.database.password",
        KernelConfigurationSchema.ConfigType.STRING,
        null,
        false,  // Optional
        KernelConfigurationSchema.Strictness.NON_STRICT,
        List.of()
        // Contains "password" → auto-redacted
    )
)
```

### Example 3: Environment Variable Override

Default: `govaryn.mymodule.feature.enabled=false` (in schema)
File: `govaryn.mymodule.feature.enabled=true` (in application.properties)
Environment: `GOVARYN_MYMODULE_FEATURE_ENABLED=false` (in CI/CD)

**Result:** `false` (environment variable wins)

### Example 4: Cross-Field Validation

```java
@Override
public void validateConfiguration(Map<String, Object> config) throws ConfigurationException {
    // If module requires an API key when online mode is enabled
    boolean online = "true".equalsIgnoreCase(
        (String) config.getOrDefault("govaryn.mymodule.online.mode", "false")
    );
    
    if (online) {
        String apiKey = (String) config.get("govaryn.mymodule.api.key");
        if (apiKey == null || apiKey.isBlank()) {
            throw new ConfigurationException(
                "govaryn.mymodule.api.key",
                "is required when online.mode is enabled"
            );
        }
    }
}
```

## Best Practices

### 1. Use Consistent Namespace Naming

```
govaryn.{modulename}.{feature}.{setting}
```

Examples:
- `govaryn.payments.api.endpoint`
- `govaryn.notifications.email.sender`
- `govaryn.cache.redis.host`

### 2. Provide Sensible Defaults

```java
// Bad: no default, always required
new ConfigKeyMetadata("govaryn.mymodule.setting", ..., null, true, ...)

// Good: sensible default, can be overridden
new ConfigKeyMetadata("govaryn.mymodule.setting", ..., "default-value", false, ...)
```

### 3. Mark Sensitive Keys Clearly

Use key names that include `password`, `token`, `secret`, `apikey`, etc. to enable automatic redaction:

```java
// ✓ Will be redacted automatically
"govaryn.mymodule.database.password"

// ✓ Will be redacted automatically
"govaryn.mymodule.api.token"

// ✗ Won't be redacted (unclear name)
"govaryn.mymodule.db.cred"

// Register custom pattern if needed
SecretRedactor.registerSecretPattern("(?i).*credential.*");
```

### 4. Cache Module Configuration at Startup

```java
@Service
public class MyService {
    private final Map<String, Object> config;

    @Autowired
    public MyService(ConfigurationManager configManager) {
        // Cache at construction time
        this.config = configManager.getNamespacedConfiguration("govaryn.mymodule");
    }
    
    public void useConfig() {
        // Use cached config (zero-overhead access)
        String setting = (String) config.get("govaryn.mymodule.setting");
    }
}
```

### 5. Validate Early, Fail Fast

```java
@Override
public void validateConfiguration(Map<String, Object> config) throws ConfigurationException {
    // Validate as early as possible in startup
    // Throw clear errors immediately
    
    Object value = config.get("govaryn.mymodule.required.setting");
    if (value == null) {
        throw new ConfigurationException(
            "govaryn.mymodule.required.setting",
            "must be configured for the module to function"
        );
    }
}
```

## Troubleshooting

### Configuration not found at runtime

1. **Check namespace:** Ensure you're using the correct namespace (e.g., `govaryn.mymodule`, not `govaryn.my-module`)
2. **Check schema registration:** Verify the schema is registered with `ConfigurationManager` before `initialize()` is called
3. **Check file placement:** Verify `application.properties` is in `src/main/resources`

### Validation errors during startup

1. **Read the error message:** It names the affected key and reason
2. **Check required vs. optional:** Is the key required in your schema?
3. **Check enum values:** For ENUM types, is the value in the allowed list?
4. **Check types:** Is the value the correct type (string, enum)?

### Secrets not being redacted

1. **Check key name:** Does it contain `password`, `token`, `secret`, etc.?
2. **Register custom pattern:** If using non-standard names, register a pattern
3. **Use `getAll()` method:** Only `getAll()` returns redacted config

### Module configuration includes unexpected keys

```java
// Returns only keys that match the requested prefix
configManager.getNamespacedConfiguration("govaryn.modulea");

// If you request another namespace, those keys are returned
configManager.getNamespacedConfiguration("govaryn.moduleb");
```

`ConfigurationManager` performs prefix filtering based on the namespace argument. It does not enforce caller identity at this layer.

Use team/module conventions to ensure each module requests only its own namespace.

## Testing Configuration

### Unit Testing

```java
@Test
void testConfigurationLoaded() {
    Map<String, Object> config = new HashMap<>();
    config.put("govaryn.mymodule.feature.enabled", "true");
    
    MyModuleConfigurationSchema schema = new MyModuleConfigurationSchema();
    
    // Validate doesn't throw
    assertDoesNotThrow(() -> schema.validateConfiguration(config));
}
```

### Integration Testing

```java
@SpringBootTest
class MyModuleIntegrationTest {
    
    @Autowired
    private ConfigurationManager configManager;
    
    @Test
    void testConfigurationInjected() {
        Map<String, Object> config = 
            configManager.getNamespacedConfiguration("govaryn.mymodule");
        
        assertThat(config).isNotEmpty();
        assertThat(config.get("govaryn.mymodule.feature.enabled"))
            .isEqualTo("true");
    }
}
```

## See Also

- [Configuration Management Architecture](./CONFIGURATION_MANAGEMENT.md) — Deep dive into design and implementation
- [KernelModule API](../kernel/src/main/java/io/govaryn/kernel/api/KernelModule.java) — Module lifecycle and initialization
- [SecretRedactor](../kernel/src/main/java/io/govaryn/kernel/config/SecretRedactor.java) — Secret detection and redaction
