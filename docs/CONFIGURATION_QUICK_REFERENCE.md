# Configuration Management - Quick Reference

## For Module Developers

### 1. Create Configuration Schema (5 min)

```java
public class MyModuleConfigurationSchema implements ModuleConfigurationSchema {

    @Override
    public String getNamespace() {
        return "govaryn.mymodule";
    }

    @Override
    public List<KernelConfigurationSchema.ConfigKeyMetadata> getKeyMetadata() {
        return List.of(
            // Format: name, type, default, required, strictness, allowedValues
            new KernelConfigurationSchema.ConfigKeyMetadata(
                "govaryn.mymodule.enabled",
                ConfigType.ENUM,
                "true",  // Default
                true,    // Required
                Strictness.STRICT,
                List.of("true", "false")
            ),
            new KernelConfigurationSchema.ConfigKeyMetadata(
                "govaryn.mymodule.api.key",
                ConfigType.STRING,
                null,    // No default
                false,   // Optional
                Strictness.NON_STRICT,
                List.of()
            )
        );
    }

    @Override
    public void validateConfiguration(Map<String, Object> config) throws ConfigurationException {
        // Optional: cross-field validation
        if ("true".equals(config.get("govaryn.mymodule.enabled"))) {
            String apiKey = (String) config.get("govaryn.mymodule.api.key");
            if (apiKey == null) {
                throw new ConfigurationException("govaryn.mymodule.api.key",
                    "is required when enabled=true");
            }
        }
    }
}
```

### 2. Register Schema (2 min)

```java
@Component
public class MyModuleConfigInit {
    @Autowired
    public MyModuleConfigInit(ConfigurationManager configManager) {
        configManager.registerModule(new MyModuleConfigurationSchema());
    }
}
```

### 3. Use Configuration (2 min)

```java
@Service
public class MyModuleService {
    
    @Autowired
    private ConfigurationManager config;
    
    public void process() {
        // Get namespace
        Map<String, Object> cfg = config.getNamespacedConfiguration("govaryn.mymodule");
        
        // Get individual values
        String apiKey = config.getString("govaryn.mymodule.api.key");
        String enabled = (String) cfg.get("govaryn.mymodule.enabled");
    }
}
```

## Configuration File Format

### application.properties

```properties
# Kernel (required)
govaryn.kernel.id=my-kernel-01
govaryn.kernel.environment=DEV
govaryn.kernel.module.mode=CLASSPATH

# Your Module
govaryn.mymodule.enabled=true
govaryn.mymodule.api.key=sk_live_1234567890abcdef
```

### Environment Variables (for secrets in CI/CD)

```bash
# Highest precedence — use for secrets
export GOVARYN_MYMODULE_API_KEY=sk_live_from_secrets_manager
export GOVARYN_MYMODULE_DB_PASSWORD=prod_password
```

## Key Concepts

| Concept | Meaning | Example |
|---------|---------|---------|
| **Namespace** | Module's config prefix | `govaryn.mymodule` |
| **ConfigType** | `ENUM` or `STRING` | `ConfigType.ENUM` |
| **Strictness** | `STRICT` (enum only) or `NON_STRICT` (any string) | `Strictness.STRICT` |
| **Default** | Value if not provided | `"true"` or `null` (no default) |
| **Required** | Must be set (or have default) | `true` or `false` |
| **Secret** | Auto-redacted in logs | Keys with `password`, `token`, etc. |

## Validation Rules

### Schema Validation (Automatic)

✅ Required key present (unless has default)  
✅ Value type matches schema  
✅ Enum values are in allowed list  
✅ String values are non-blank  

### Custom Validation (Optional)

Override `validateConfiguration()` for:
- Cross-field validation
- Conditional requirements
- Business logic validation

## Configuration Precedence (Remember!)

```
HIGHEST:    Environment Variables
            GOVARYN_MYMODULE_SETTING=value
           ↓
MEDIUM:     Files (application.properties)
            govaryn.mymodule.setting=value
           ↓
LOWEST:     Schema Defaults
            new ConfigKeyMetadata("...", default="value", ...)
```

Environment always wins! Use this for secrets in production.

## Testing Configuration

### Quick Unit Test

```java
@Test
void testConfiguration() {
    Map<String, Object> config = Map.of(
        "govaryn.mymodule.enabled", "true",
        "govaryn.mymodule.api.key", "test-key"
    );
    
    MyModuleConfigurationSchema schema = new MyModuleConfigurationSchema();
    assertDoesNotThrow(() -> schema.validateConfiguration(config));
}
```

### Integration Test with Spring

```java
@SpringBootTest
class ConfigurationIT {
    @Autowired private ConfigurationManager configManager;
    
    @Test
    void testModuleConfigLoaded() {
        Map<String, Object> cfg = 
            configManager.getNamespacedConfiguration("govaryn.mymodule");
        assertNotEmpty(cfg);
    }
}
```

## Troubleshooting Checklist

| Problem | Solution |
|---------|----------|
| "Required key is missing" | Add key to application.properties or set env var |
| "must be one of [...]" | Check enum value (case-sensitive) |
| "Key not found at runtime" | Verify namespace matches (e.g., `govaryn.mymodule`) |
| "Secret visible in logs" | Key name must contain `password`, `token`, etc. |
| "Module can't see its config" | Schema not registered? Call `registerModule()` early |

## Best Practices Checklist

- [ ] Use consistent namespace: `govaryn.{modulename}.{feature}`
- [ ] Provide sensible defaults (unless truly required)
- [ ] Name sensitive keys clearly (`password`, `token`, `secret`, `apikey`)
- [ ] Implement custom validation for complex rules
- [ ] Cache config at startup (zero runtime overhead)
- [ ] Register schema before ConfigurationManager initializes
- [ ] Use environment variables for production secrets

## API Reference

### ConfigurationManager

```java
// Get all config for a module
Map<String, Object> getNamespacedConfiguration(String namespace)

// Get single value as object
Object get(String key)

// Get single value as string
String getString(String key)

// Get all config (redacted for safety)
Map<String, Object> getAll()

// Get all config (unredacted — use internally only)
Map<String, Object> getAllUnredacted()

// Check if key is allowed
boolean isKeyAllowed(String key)

// Initialize (called automatically by kernel)
void initialize()

// Register module schema
void registerModule(ModuleConfigurationSchema schema)
```

### ModuleConfigurationSchema

```java
// Required: namespace prefix
String getNamespace()

// Required: list of keys
List<ConfigKeyMetadata> getKeyMetadata()

// Optional: custom validation
void validateConfiguration(Map<String, Object> config) throws ConfigurationException
```

### ConfigKeyMetadata

```java
record ConfigKeyMetadata(
    String name,              // "govaryn.module.key"
    ConfigType type,          // STRING or ENUM
    Object defaultValue,      // null for no default
    boolean required,         // true/false
    Strictness strictness,    // STRICT or NON_STRICT
    List<String> allowedValues // for ENUM types
)
```

### SecretRedactor

```java
// Check if key is a secret
boolean isSecret(String key)

// Redact value if secret
Object redactIfSecret(String key, Object value)

// Redact all secrets in map
Map<String, Object> redactSecrets(Map<String, Object> config)

// Get redacted string representation
String toRedactedString(Map<String, Object> config)

// Register custom pattern
void registerSecretPattern(String regexPattern)
```

## Links

- [Full Architecture](CONFIGURATION_MANAGEMENT.md)
- [Integration Guide](CONFIGURATION_INTEGRATION_GUIDE.md)
- [Visual Diagrams](CONFIGURATION_DIAGRAMS.md)

