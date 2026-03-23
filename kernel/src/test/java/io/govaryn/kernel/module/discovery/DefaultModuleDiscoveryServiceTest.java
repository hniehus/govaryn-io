package io.govaryn.kernel.module.discovery;

import io.govaryn.kernel.api.KernelContext;
import io.govaryn.kernel.api.KernelModule;
import io.govaryn.kernel.config.GovarynKernelProperties;
import io.govaryn.kernel.config.KernelEnvironment;
import io.govaryn.kernel.config.ModuleMode;
import io.govaryn.kernel.module.ModuleMetadata;
import io.govaryn.kernel.module.ModuleType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DefaultModuleDiscoveryService Tests")
@ExtendWith(OutputCaptureExtension.class)
class DefaultModuleDiscoveryServiceTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Should discover classpath modules in CLASSPATH mode")
    void shouldDiscoverClasspathModules() {
        GovarynKernelProperties properties = baseProperties();
        properties.setModuleMode(ModuleMode.CLASSPATH);

        KernelModule module = new TestKernelModule();
        DefaultModuleDiscoveryService discoveryService = new DefaultModuleDiscoveryService(
            providerFor(module),
            properties
        );

        List<ModuleDiscoveryCandidate> candidates = discoveryService.discover();

        assertEquals(1, candidates.size());
        ModuleDiscoveryCandidate candidate = candidates.getFirst();
        assertEquals(ModuleDiscoverySource.CLASSPATH, candidate.source());
        assertTrue(candidate.loadableInCurrentRuntime());
        assertEquals("test-module", candidate.metadata().moduleId());
    }

    @Test
    @DisplayName("Should discover plugin manifest candidates in PLUGIN_FOLDER mode")
    void shouldDiscoverPluginManifestCandidates() throws IOException {
        Path pluginDir = tempDir.resolve("plugins");
        Files.createDirectories(pluginDir.resolve("sample-module"));
        Files.writeString(
            pluginDir.resolve("sample-module").resolve("module.json"),
            """
            {
              "moduleContractVersion": "1.0.0",
              "moduleId": "plugin-audit",
              "moduleName": "Plugin Audit Module",
              "moduleVersion": "1.1.0",
              "requiredKernelApiVersion": "^1.0.0",
              "moduleType": "observability",
              "entryPoint": "io.govaryn.plugins.audit.PluginAuditModule"
            }
            """
        );

        GovarynKernelProperties properties = baseProperties();
        properties.setModuleMode(ModuleMode.PLUGIN_FOLDER);
        properties.setModulePluginDirectory(pluginDir.toString());

        DefaultModuleDiscoveryService discoveryService = new DefaultModuleDiscoveryService(
            providerFor(),
            properties
        );

        List<ModuleDiscoveryCandidate> candidates = discoveryService.discover();

        assertEquals(1, candidates.size());
        ModuleDiscoveryCandidate candidate = candidates.getFirst();
        assertEquals(ModuleDiscoverySource.MANIFEST_SCAN, candidate.source());
        assertFalse(candidate.loadableInCurrentRuntime());
        assertEquals("plugin-audit", candidate.metadata().moduleId());
        assertEquals("Plugin Audit Module", candidate.metadata().moduleName());
    }

    @Test
    @DisplayName("Should log cause for invalid plugin manifest and skip candidate")
    void shouldLogCauseForInvalidPluginManifest(CapturedOutput output) throws IOException {
        Path pluginDir = tempDir.resolve("plugins");
        Files.createDirectories(pluginDir.resolve("broken-module"));
        Path invalidManifest = pluginDir.resolve("broken-module").resolve("module.json");
        Files.writeString(
            invalidManifest,
            """
            {
              "moduleContractVersion": "1.0.0",
              "moduleName": "Broken Module",
              "moduleVersion": "1.1.0",
              "requiredKernelApiVersion": "^1.0.0",
              "moduleType": "feature",
              "entryPoint": "io.govaryn.plugins.broken.BrokenModule"
            }
            """
        );

        GovarynKernelProperties properties = baseProperties();
        properties.setModuleMode(ModuleMode.PLUGIN_FOLDER);
        properties.setModulePluginDirectory(pluginDir.toString());

        DefaultModuleDiscoveryService discoveryService = new DefaultModuleDiscoveryService(
            providerFor(),
            properties
        );

        List<ModuleDiscoveryCandidate> candidates = discoveryService.discover();

        assertTrue(candidates.isEmpty());
        assertTrue(output.getOut().contains("Skipping invalid module manifest"));
        assertTrue(output.getOut().contains(invalidManifest.toAbsolutePath().toString()));
        assertTrue(output.getOut().contains("Missing required field: moduleId"));
    }

    private static GovarynKernelProperties baseProperties() {
        GovarynKernelProperties properties = new GovarynKernelProperties();
        properties.setId("test-kernel");
        properties.setEnvironment(KernelEnvironment.DEV);
        properties.setModulePluginDirectory("./plugins");
        return properties;
    }

    @SafeVarargs
    private static ObjectProvider<KernelModule> providerFor(KernelModule... modules) {
        StaticListableBeanFactory factory = new StaticListableBeanFactory();
        for (int i = 0; i < modules.length; i++) {
            factory.addBean("module" + i, modules[i]);
        }
        return factory.getBeanProvider(KernelModule.class);
    }

    private static class TestKernelModule implements KernelModule {
        @Override
        public ModuleMetadata metadata() {
            return ModuleMetadata.minimal(
                "test-module",
                "Test Module",
                "1.0.0",
                "^1.0.0",
                ModuleType.FEATURE,
                getClass().getName()
            );
        }

        @Override
        public void init(KernelContext context) {
            // no-op test module
        }
    }
}
