package io.govaryn.kernel.module.discovery;

import io.govaryn.kernel.api.KernelModule;
import io.govaryn.kernel.config.ModuleFailurePolicyAction;
import io.govaryn.kernel.config.GovarynKernelProperties;
import io.govaryn.kernel.config.ModuleMode;
import io.govaryn.kernel.module.ModuleCapabilities;
import io.govaryn.kernel.module.ModuleFailurePolicy;
import io.govaryn.kernel.module.ModuleMetadata;
import io.govaryn.kernel.module.ModuleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class DefaultModuleDiscoveryService implements ModuleDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(DefaultModuleDiscoveryService.class);
    private static final String DEFAULT_MANIFEST_NAME = "module.json";
    private static final int MAX_SCAN_DEPTH = 8;
    private static final long MAX_MANIFEST_SIZE_BYTES = 1024 * 1024;
    private static final JsonParser JSON_PARSER = JsonParserFactory.getJsonParser();

    private final List<KernelModule> classpathModules;
    private final GovarynKernelProperties properties;

    public DefaultModuleDiscoveryService(
        ObjectProvider<KernelModule> modules,
        GovarynKernelProperties properties
    ) {
        this.classpathModules = modules.orderedStream().toList();
        this.properties = properties;
    }

    @Override
    public List<ModuleDiscoveryCandidate> discover() {
        List<ModuleDiscoveryCandidate> candidates = new ArrayList<>(discoverClasspathCandidates());

        if (properties.getModuleMode() == ModuleMode.PLUGIN_FOLDER) {
            candidates.addAll(discoverPluginDirectoryCandidates());
        }

        return List.copyOf(candidates);
    }

    private List<ModuleDiscoveryCandidate> discoverClasspathCandidates() {
        return classpathModules.stream()
            .map(module -> new ModuleDiscoveryCandidate(
                module.metadata(),
                ModuleDiscoverySource.CLASSPATH,
                module.getClass().getName(),
                true
            ))
            .toList();
    }

    private List<ModuleDiscoveryCandidate> discoverPluginDirectoryCandidates() {
        Path pluginDir = Path.of(properties.getModulePluginDirectory());
        if (!Files.exists(pluginDir)) {
            log.info("Plugin directory does not exist: {}", pluginDir.toAbsolutePath());
            return List.of();
        }
        if (!Files.isDirectory(pluginDir)) {
            log.warn("Configured plugin path is not a directory: {}", pluginDir.toAbsolutePath());
            return List.of();
        }

        List<ModuleDiscoveryCandidate> candidates = new ArrayList<>();
        try (var paths = Files.walk(pluginDir, MAX_SCAN_DEPTH)) {
            paths.filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().equalsIgnoreCase(DEFAULT_MANIFEST_NAME))
                .forEach(path -> parseManifest(path).ifPresent(candidates::add));
        } catch (IOException e) {
            log.warn("Failed to scan plugin directory '{}': {}", sanitizeForLog(pluginDir.toAbsolutePath().toString()), sanitizeForLog(e.getMessage()));
        }

        return List.copyOf(candidates);
    }

    private java.util.Optional<ModuleDiscoveryCandidate> parseManifest(Path manifestPath) {
        try {
            long size = Files.size(manifestPath);
            if (size > MAX_MANIFEST_SIZE_BYTES) {
                throw new IllegalArgumentException("Manifest exceeds max size of " + MAX_MANIFEST_SIZE_BYTES + " bytes");
            }
            Map<String, Object> root = JSON_PARSER.parseMap(Files.readString(manifestPath));
            ModuleMetadata metadata = new ModuleMetadata(
                requiredText(root, "moduleContractVersion"),
                requiredText(root, "moduleId"),
                requiredText(root, "moduleName"),
                requiredText(root, "moduleVersion"),
                requiredText(root, "requiredKernelApiVersion"),
                parseModuleType(requiredText(root, "moduleType")),
                requiredText(root, "entryPoint"),
                optionalText(root, "description"),
                optionalText(root, "author"),
                optionalText(root, "license"),
                optionalText(root, "homepage"),
                parseCapabilities(root),
                parseFailurePolicy(root.get("failurePolicy")),
                optionalText(root, "configSchemaRef"),
                parseStringArray(root.get("healthChecks"))
            );

            return java.util.Optional.of(new ModuleDiscoveryCandidate(
                metadata,
                ModuleDiscoverySource.MANIFEST_SCAN,
                manifestPath.toAbsolutePath().toString(),
                false
            ));
        } catch (Exception e) {
            log.warn(
                "Skipping invalid module manifest '{}': {}",
                sanitizeForLog(manifestPath.toAbsolutePath().toString()),
                sanitizeForLog(e.getMessage())
            );
            return java.util.Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> parseStringArray(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof String string && !string.isBlank()) {
                values.add(string);
            }
        }
        return List.copyOf(values);
    }

    private static ModuleCapabilities parseCapabilities(Map<String, Object> root) {
        List<String> provided = parseStringArray(root.get("providedCapabilities"));
        if (provided.isEmpty()) {
            // Backward-compatible fallback for older manifests.
            provided = parseStringArray(root.get("capabilities"));
        }
        List<String> required = parseStringArray(root.get("requiredCapabilities"));
        List<String> optional = parseStringArray(root.get("optionalCapabilities"));
        return new ModuleCapabilities(provided, required, optional);
    }

    @SuppressWarnings("unchecked")
    private static ModuleFailurePolicy parseFailurePolicy(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return ModuleFailurePolicy.defaults();
        }
        String initRaw = toNonBlankString(map.get("onInitializationFailure"));
        String runtimeRaw = toNonBlankString(map.get("onRuntimeFailure"));
        if (initRaw == null || runtimeRaw == null) {
            throw new IllegalArgumentException("failurePolicy must define onInitializationFailure and onRuntimeFailure");
        }
        return new ModuleFailurePolicy(
            ModuleFailurePolicyAction.valueOf(initRaw.trim().toUpperCase(Locale.ROOT)),
            ModuleFailurePolicyAction.valueOf(runtimeRaw.trim().toUpperCase(Locale.ROOT))
        );
    }

    private static ModuleType parseModuleType(String raw) {
        String normalized = raw
            .trim()
            .replace('-', '_')
            .toUpperCase(Locale.ROOT);
        return ModuleType.valueOf(normalized);
    }

    private static String requiredText(Map<String, Object> root, String fieldName) {
        String value = optionalText(root, fieldName);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required field: " + fieldName);
        }
        return value;
    }

    private static String optionalText(Map<String, Object> root, String fieldName) {
        return toNonBlankString(root.get(fieldName));
    }

    private static String toNonBlankString(Object value) {
        if (!(value instanceof String text) || text.isBlank()) {
            return null;
        }
        return text;
    }

    private static String sanitizeForLog(String value) {
        if (value == null) {
            return "null";
        }
        return value.replaceAll("[\\r\\n\\t\\x00-\\x1F]", " ").trim();
    }
}
