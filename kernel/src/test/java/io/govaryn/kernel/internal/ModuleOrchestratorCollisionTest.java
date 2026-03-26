package io.govaryn.kernel.internal;

import io.govaryn.kernel.api.KernelModule;
import io.govaryn.kernel.config.GovarynKernelProperties;
import io.govaryn.kernel.config.KernelEnvironment;
import io.govaryn.kernel.config.ModuleMode;
import io.govaryn.kernel.module.InMemoryModuleRegistry;
import io.govaryn.kernel.module.ModuleMetadata;
import io.govaryn.kernel.module.ModuleType;
import io.govaryn.kernel.module.discovery.ModuleDiscoveryCandidate;
import io.govaryn.kernel.module.discovery.ModuleDiscoveryService;
import io.govaryn.kernel.module.discovery.ModuleDiscoverySource;
import io.govaryn.kernel.module.graph.ModuleDependencyGraphValidator;
import io.govaryn.kernel.module.identity.ModuleIdentityCollisionDetector;
import io.govaryn.kernel.module.validation.ModuleValidationReport;
import io.govaryn.kernel.module.validation.ModuleValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.DefaultApplicationArguments;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ModuleOrchestrator Collision Tests")
class ModuleOrchestratorCollisionTest {

    @Test
    @DisplayName("Should abort startup when two modules share the same moduleId")
    void shouldAbortStartupOnModuleIdCollision() {
        GovarynKernelProperties properties = new GovarynKernelProperties();
        properties.setId("test-kernel");
        properties.setEnvironment(KernelEnvironment.DEV);
        properties.setModuleMode(ModuleMode.CLASSPATH);
        properties.setModulePluginDirectory("./plugins");

        ModuleDiscoveryCandidate classpathCandidate = new ModuleDiscoveryCandidate(
            ModuleMetadata.minimal(
                "shared-id",
                "ClasspathModule",
                "1.0.0",
                "^1.0.0",
                ModuleType.FEATURE,
                "io.govaryn.modules.ClasspathModule"
            ),
            ModuleDiscoverySource.CLASSPATH,
            "io.govaryn.modules.ClasspathModule",
            true
        );

        ModuleDiscoveryCandidate pluginCandidate = new ModuleDiscoveryCandidate(
            ModuleMetadata.minimal(
                "shared-id",
                "PluginModule",
                "1.0.0",
                "^1.0.0",
                ModuleType.FEATURE,
                "io.govaryn.plugins.PluginModule"
            ),
            ModuleDiscoverySource.MANIFEST_SCAN,
            "/plugins/plugin-module/module.json",
            false
        );

        ModuleDiscoveryService discoveryService = () -> List.of(classpathCandidate, pluginCandidate);
        ModuleValidator moduleValidator = (candidates, version) -> candidates.stream()
            .map(c -> new ModuleValidationReport(
                c.metadata().moduleId(),
                c.metadata().moduleName(),
                c.source(),
                c.origin(),
                true,
                List.of()
            ))
            .toList();

        ModuleOrchestrator orchestrator = new ModuleOrchestrator(
            emptyModuleProvider(),
            properties,
            discoveryService,
            moduleValidator,
            new InMemoryModuleRegistry(),
            new ModuleIdentityCollisionDetector(),
            new ModuleDependencyGraphValidator(),
            new ModuleInitializationExecutor(),
            "1.2.0"
        );

        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> orchestrator.run(new DefaultApplicationArguments(new String[0]))
        );

        assertTrue(ex.getMessage().contains("Module identity collision detected"));
        assertTrue(ex.getMessage().contains("shared-id"));
        assertTrue(ex.getMessage().contains("ClasspathModule"));
        assertTrue(ex.getMessage().contains("PluginModule"));
        assertTrue(ex.getMessage().contains("io.govaryn.modules.ClasspathModule"));
        assertTrue(ex.getMessage().contains("/plugins/plugin-module/module.json"));
    }

    private static ObjectProvider<KernelModule> emptyModuleProvider() {
        StaticListableBeanFactory factory = new StaticListableBeanFactory();
        return factory.getBeanProvider(KernelModule.class);
    }
}
