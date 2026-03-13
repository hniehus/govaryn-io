# Govaryn Kernel Skeleton

Initial runnable project skeleton for the Govaryn Kernel runtime.

## Stack
- Kotlin
- Spring Boot 4.0.3
- Maven
- Java 25

## Project layout
- `pom.xml` — parent multi-module build
- `kernel/` — runnable kernel process
- `config/application.properties.example` — minimal external config template

## Minimal local run
1. Copy the template:
   ```bash
   cp config/application.properties.example config/application.properties
   ```
2. Start the kernel:
   ```bash
   mvn -pl kernel spring-boot:run -Dspring-boot.run.arguments="--spring.config.additional-location=optional:file:./config/"
   ```
3. Check health:
   ```bash
   curl http://localhost:8080/health
   ```

## Build
```bash
mvn clean install
```

That installs the versioned `govaryn-kernel` artifact into your local Maven repository.
