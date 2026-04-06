# Govaryn Kernel

Runnable kernel runtime for Govaryn IO.

## Stack
- Java 21
- Spring Boot 4.0.3
- Maven

## Project layout
- `pom.xml` — parent multi-module build
- `kernel/` — runnable kernel process
- `modules/` — shared example module sources used by kernel scenarios
- `config/application.properties.example` — minimal external configuration template

## Minimal local run
1. Copy the template (first run only):
   ```bash
   cp config/application.properties.example config/application.properties
   ```
2. Start the kernel:
   ```bash
   ./run.sh
   ```
   Alternative:
   ```bash
   mvn -pl kernel spring-boot:run -Dspring-boot.run.arguments="--spring.config.additional-location=optional:file:$(pwd)/config/"
   ```
3. Check endpoints:
   ```bash
   curl http://localhost:8080/health
   curl http://localhost:8080/actuator/health
   ```

## Build
```bash
mvn clean install
```

This installs the versioned `govaryn-kernel` artifact into your local Maven repository.

## Reference docs
- [Reference modules and negative examples](../docs/modules/REFERENCE_MODULES.md)
- [Module startup and registration runbook](../docs/operations/MODULE_STARTUP_AND_REGISTRATION.md)
