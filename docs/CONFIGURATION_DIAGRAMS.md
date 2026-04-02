# Configuration Management System Diagram

## System Overview

```
┌───────────────────────────────────────────────────────────────────┐
│                    GOVARYN KERNEL STARTUP                          │
└───────────────────┬─────────────────────────────────────────────────┘
                    │
                    ▼
          ┌─────────────────────┐
          │  Spring Boot Init   │
          │  (Load Beans)       │
          └─────────┬───────────┘
                    │
                    ▼
    ┌───────────────────────────────────────┐
    │  ConfigurationManager Created by      │
    │  Spring Dependency Injection          │
    │  (as @Component)                      │
    └─────────┬───────────────────────────────┘
              │
              ▼
    ┌──────────────────────────────────────────┐
    │  Modules Register Their Schemas          │
    │  (Early in component creation phase)     │
    │                                          │
    │  configManager.registerModule(schema)    │
    │  - Kernel Schema (auto-registered)       │
    │  - Module A Schema                       │
    │  - Module B Schema                       │
    │  - ...                                   │
    └─────────┬──────────────────────────────────┘
              │
              ▼
    ┌──────────────────────────────────────────┐
    │  KernelApplication.initialize()          │
    │                                          │
    │  configManager.initialize()              │
    └────────┬─────────────────────────────────┘
             │
      ┌──────┴──────────────────────────────────┐
      │                                          │
      ▼                                          ▼
┌───────────────────┐                  ┌──────────────────┐
│  Load Defaults    │                  │ Load Externals   │
│  From Schemas     │                  │ - application.   │
│                   │                  │   properties     │
│ govaryn.kernel.*  │                  │ - Environment:   │
│   (id, env, mode) │                  │   Converted      │
│                   │                  │   SCREAMING_CASE │
│ govaryn.module.*  │                  │   to dot.case    │
│   (if registered) │                  │                  │
└───────┬───────────┘                  └────────┬─────────┘
        │                                        │
        └─────────────┬─────────────────────────┘
                      │
                      ▼
    ┌─────────────────────────────────────────┐
    │  Merge Configuration (Precedence)       │
    │                                         │
    │  1. Start with defaults                 │
    │  2. Override with external config       │
    │  3. Override with environment variables │
    │     (HIGHEST PRIORITY)                  │
    └─────────┬───────────────────────────────┘
              │
              ▼
┌──────────────────────────────────────────────────┐
│  Validate Configuration (Fast-Fail)              │
│                                                  │
│  FOR EACH KEY IN SCHEMA:                         │
│  ├─ Check required keys are present             │
│  ├─ Check types match (STRING, ENUM)            │
│  ├─ Check enum values are in allowed list       │
│  ├─ Check module custom validators              │
│  │                                              │
│  IF ANY ERROR:                                   │
│  └─ Log error WITH KEY NAME                     │
│  └─ Throw ConfigurationException                │
│  └─ Exit(1) — FAST-FAIL                         │
└─────────┬──────────────────────────────────────┘
          │
          ├─ SUCCESS? ───────────┐
          │                       │ FAILURE?
          ▼                       │
┌──────────────────────┐         ▼
│ Ready to Serve       │  ┌─────────────────────┐
│ Configuration to     │  │ Startup FAILED      │
│ Modules             │  │ Kernel EXIT(1)      │
└──────────────────────┘  └─────────────────────┘
```

## Configuration Precedence Flow

```
┌──────────────────────────────────────────────┐
│  Configuration Sources                       │
└──────────────────────────────────────────────┘
         │
    ┌────┴─────────────────┬────────────────┐
    │                      │                │
    ▼                      ▼                ▼
┌─────────────┐    ┌────────────────┐    ┌──────────────┐
│  Defaults   │    │  External      │    │ Environment  │
│             │    │  Files         │    │ Variables    │
│ In Schemas  │    │                │    │              │
│ (LOWEST     │    │ application.   │    │ SCREAMING    │
│  PRIORITY)  │    │ properties     │    │ CASE_NAMES   │
│             │    │ app config/    │    │              │
│ · id        │    │ custom files   │    │ (HIGHEST     │
│ · env       │    │                │    │  PRIORITY)   │
│ · defaults  │    │ (MEDIUM        │    │              │
│             │    │  PRIORITY)     │    │              │
└─────────────┘    └────────────────┘    └──────────────┘
    │                      │                │
    │ Precedence: (3 > 2 > 1)             │
    └──────────────────────┼───────────────┘
                           │
                           ▼
               ┌──────────────────────┐
               │  Merged Config Map   │
               │                      │
               │ {key: value}         │
               │ (Final values after  │
               │  all overrides)      │
               └──────────────────────┘
```

## Namespace Filtering Behavior

`ConfigurationManager.getNamespacedConfiguration(namespace)` filters keys by prefix.

```
ALL KEYS IN MERGED MAP
  govaryn.kernel.id
  govaryn.kernel.environment
  govaryn.modulea.enabled
  govaryn.modulea.db.password
  govaryn.moduleb.enabled
  govaryn.moduleb.api.token
           │
           │ getNamespacedConfiguration("govaryn.modulea")
           ▼
RETURNS
  govaryn.modulea.enabled
  govaryn.modulea.db.password
```

Note: filtering is based on the requested namespace argument. Caller identity enforcement is not implemented in this layer.

## Secret Redaction Flow

```
┌────────────────────────────────────────┐
│  Configuration Value with Secrets      │
│                                        │
│  {                                     │
│    "app.name": "MyApp",               │
│    "db.host": "localhost",            │
│    "db.password": "super-secret",     │ ◄──── SECRET
│    "api.key": "api-token-xyz",        │ ◄──── SECRET
│    "cache.ttl": "3600"                │
│  }                                     │
└────────────────────┬───────────────────┘
                     │
                     ▼
        ┌─────────────────────────────┐
        │  SecretRedactor.isSecret()  │
        │                             │
        │  Pattern Matching:          │
        │  ├─ password ✓              │
        │  ├─ token ✓                 │
        │  ├─ secret ✓                │
        │  ├─ apikey ✓                │
        │  ├─ credential ✓            │
        │  ├─ auth ✓                  │
        │  └─ custom patterns...      │
        └─────────────┬───────────────┘
                      │
                      ▼
    ┌──────────────────────────────────┐
    │  Redact (Replace with *REDACTED) │
    │                                  │
    │  {                               │
    │    "app.name": "MyApp",          │
    │    "db.host": "localhost",       │
    │    "db.password": "***REDACTED***",
    │    "api.key": "***REDACTED***",  │
    │    "cache.ttl": "3600"           │
    │  }                               │
    └──────────────────────────────────┘
              │
              ▼
    ┌──────────────────────────────┐
    │  Safe for Logging            │
    │  Safe for Export             │
    │  Safe for Diagnostics        │
    │                              │
    │  (Secrets remain hidden)     │
    └──────────────────────────────┘
```

## Validation Error Reporting

```
┌──────────────────────────────────────────┐
│  Configuration Validation Check          │
└──────────────────────┬───────────────────┘
                       │
        ┌──────────────┼──────────────┐
        │              │              │
        ▼              ▼              ▼
    Missing       Invalid          Invalid
    Required      Enum Value       Type
    Key           
        │              │              │
        │              │              │
        ▼              ▼              ▼
    ┌───────┐     ┌────────┐     ┌──────┐
    │ Error │     │ Error  │     │Error │
    │       │     │        │     │      │
    │"conf- │     │"config-│     │"conf-│
    │igurat-│     │ error  │     │igur- │
    │ion    │     │for key │     │ation │
    │error  │     │'govaryn│     │error │
    │for    │     │.kernel │     │for   │
    │key    │     │.envir- │     │key   │
    │'govar-│     │onment' │     │'gova-│
    │yn.    │     │: must  │     │ryn.  │
    │kernel │     │be one  │     │kern- │
    │.id':  │     │of [DEV,│     │el.id │
    │is     │     │STAGE,  │     │':    │
    │requir-│     │PROD],  │     │must  │
    │ed but │     │got     │     │be a  │
    │not    │     │'STAGING│     │string│
    │provid-│     │'"      │     │"     │
    │ed"    │     │        │     │      │
    └───┬───┘     └────┬───┘     └──┬───┘
        │              │            │
        └──────────────┼────────────┘
                       │
    ┌──────────────────┴──────────────────┐
    │                                     │
    ▼                                     ▼
┌──────────────────────────────┐   ┌──────────────────┐
│  Log All Errors              │   │ Throw            │
│  (With Key Names)            │   │ ConfigurationEx- │
│                              │   │ ception          │
│  "Configuration validation   │   │                  │
│   failed with N error(s):    │   │ (Kernel catches, │
│   - error 1                  │   │  logs, exits 1)  │
│   - error 2                  │   │                  │
│   - error 3"                 │   │ FAST FAIL        │
│                              │   │ ════════════     │
└──────────────────────────────┘   └──────────────────┘
```

## Data Flow: Configuration Request from Module

```
┌─────────────────────────────────┐
│  Module Service/Component       │
│  (e.g., PaymentService)         │
│                                 │
│  @Autowired                     │
│  ConfigurationManager configMgr │
└────────────────┬────────────────┘
                 │
                 │ .getNamespacedConfiguration(
                 │    "govaryn.payments")
                 │
                 ▼
    ┌────────────────────────────┐
    │ ConfigurationManager       │
    │                            │
    │ Filter all keys that start │
    │ with "govaryn.payments."   │
    │                            │
    │ Return Map<String, Object> │
    │ containing ONLY:           │
    │ - govaryn.payments.enabled │
    │ - govaryn.payments.api.url │
    │ - govaryn.payments.api.key │
    │ - govaryn.payments.timeout │
    │                            │
    │ Does NOT include:          │
    │ - govaryn.kernel.*         │
    │ - govaryn.othermodule.*    │
    └────────────┬───────────────┘
                 │
                 │ Map<String, Object>
                 │ (from module namespace)
                 │
                 ▼
    ┌───────────────────────────┐
    │ Module Gets Its Config    │
    │                           │
    │ paymentsConfig.get(       │
    │   "govaryn.payments.      │
    │    api.key")              │
    │                           │
    │ Returns: "sk_live_abc..." │
    └───────────────────────────┘
```

---

For more details, see:
- [Configuration Management Architecture](./CONFIGURATION_MANAGEMENT.md)
- [Configuration Management Integration Guide](./CONFIGURATION_INTEGRATION_GUIDE.md)
- [Configuration Management Quick Reference](./CONFIGURATION_QUICK_REFERENCE.md)
