package io.govaryn.kernel.module;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public record ModuleMetadata(
    String moduleContractVersion,
    String moduleId,
    String moduleName,
    String moduleVersion,
    String requiredKernelApiVersion,
    ModuleType moduleType,
    String entryPoint,
    String description,
    String author,
    String license,
    String homepage,
    ModuleCapabilities capabilities,
    ModuleFailurePolicy failurePolicy,
    String configSchemaRef,
    List<String> healthChecks
) {
    private static final Pattern MODULE_ID_PATTERN = Pattern.compile("^[a-z][a-z0-9-]{2,63}$");
    private static final Pattern HTTPS_URL_PATTERN = Pattern.compile("^https://.+");

    public ModuleMetadata {
        requireNonBlank(moduleContractVersion, "moduleContractVersion");
        requireNonBlank(moduleId, "moduleId");
        requireNonBlank(moduleName, "moduleName");
        requireNonBlank(moduleVersion, "moduleVersion");
        requireNonBlank(requiredKernelApiVersion, "requiredKernelApiVersion");
        requireNonBlank(entryPoint, "entryPoint");

        if (!MODULE_ID_PATTERN.matcher(moduleId).matches()) {
            throw new IllegalArgumentException("moduleId must match ^[a-z][a-z0-9-]{2,63}$");
        }
        if (moduleType == null) {
            throw new IllegalArgumentException("moduleType must not be null");
        }
        if (homepage != null && !homepage.isBlank() && !HTTPS_URL_PATTERN.matcher(homepage).matches()) {
            throw new IllegalArgumentException("homepage must start with https://");
        }

        capabilities = capabilities == null ? ModuleCapabilities.empty() : capabilities;
        failurePolicy = failurePolicy == null ? ModuleFailurePolicy.defaults() : failurePolicy;
        healthChecks = healthChecks == null ? List.of() : List.copyOf(healthChecks);
    }

    public static ModuleMetadata minimal(
        String moduleId,
        String moduleName,
        String moduleVersion,
        String requiredKernelApiVersion,
        ModuleType moduleType,
        String entryPoint
    ) {
        return new ModuleMetadata(
            "1.0.0",
            moduleId,
            moduleName,
            moduleVersion,
            requiredKernelApiVersion,
            moduleType,
            entryPoint,
            null,
            null,
            null,
            null,
            new ModuleCapabilities(List.of("module.default"), List.of(), List.of()),
            ModuleFailurePolicy.defaults(),
            null,
            List.of()
        );
    }

    private static void requireNonBlank(String value, String fieldName) {
        if (Objects.isNull(value) || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
