package io.govaryn.kernel.module.discovery;

import io.govaryn.kernel.api.KernelModule;
import io.govaryn.kernel.config.GovarynKernelProperties;
import io.govaryn.kernel.config.ModuleMode;
import io.govaryn.kernel.module.ModuleDependency;
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
        try (var paths = Files.walk(pluginDir)) {
            paths.filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().equalsIgnoreCase(DEFAULT_MANIFEST_NAME))
                .forEach(path -> parseManifest(path).ifPresent(candidates::add));
        } catch (IOException e) {
            log.warn("Failed to scan plugin directory '{}': {}", pluginDir.toAbsolutePath(), e.getMessage());
        }

        return List.copyOf(candidates);
    }

    private java.util.Optional<ModuleDiscoveryCandidate> parseManifest(Path manifestPath) {
        try {
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
                parseStringArray(root.get("capabilities")),
                parseDependencies(root.get("dependencies")),
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
            log.warn("Skipping invalid module manifest '{}': {}", manifestPath.toAbsolutePath(), e.getMessage());
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

    @SuppressWarnings("unchecked")
    private static List<ModuleDependency> parseDependencies(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<ModuleDependency> dependencies = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> dependencyMap) {
                String moduleId = toNonBlankString(dependencyMap.get("moduleId"));
                String versionRange = toNonBlankString(dependencyMap.get("versionRange"));
                boolean optional = dependencyMap.get("optional") instanceof Boolean b && b;
                if (moduleId != null && versionRange != null) {
                    dependencies.add(new ModuleDependency(moduleId, versionRange, optional));
                }
            }
        }
        return List.copyOf(dependencies);
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
}
