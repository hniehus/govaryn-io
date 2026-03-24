package io.govaryn.kernel.internal;

import io.govaryn.kernel.api.KernelModule;
import io.govaryn.kernel.config.GovarynKernelProperties;
import io.govaryn.kernel.config.KernelEnvironment;
import io.govaryn.kernel.config.ModuleMode;
import io.govaryn.kernel.module.InMemoryModuleRegistry;
import io.govaryn.kernel.module.ModuleLifecycleState;
import io.govaryn.kernel.module.ModuleMetadata;
import io.govaryn.kernel.module.ModuleType;
import io.govaryn.kernel.module.discovery.ModuleDiscoveryCandidate;
import io.govaryn.kernel.module.discovery.ModuleDiscoveryService;
import io.govaryn.kernel.module.discovery.ModuleDiscoverySource;
import io.govaryn.kernel.module.graph.ModuleDependencyGraphValidator;
import io.govaryn.kernel.module.identity.ModuleIdentityCollisionDetector;
import io.govaryn.kernel.module.validation.ModuleValidationIssue;
import io.govaryn.kernel.module.validation.ModuleValidationReport;
import io.govaryn.kernel.module.validation.ModuleValidationSeverity;
import io.govaryn.kernel.module.validation.ModuleValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.DefaultApplicationArguments;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ModuleOrchestrator Registration Tests")
class ModuleOrchestratorRegistrationTest {

    @Test
    @DisplayName("Should register only valid module candidates")
    void shouldRegisterOnlyValidCandidates() {
        GovarynKernelProperties properties = new GovarynKernelProperties();
        properties.setId("test-kernel");
        properties.setEnvironment(KernelEnvironment.DEV);
        properties.setModuleMode(ModuleMode.CLASSPATH);
        properties.setModulePluginDirectory("./plugins");

        ModuleDiscoveryCandidate validCandidate = new ModuleDiscoveryCandidate(
            ModuleMetadata.minimal(
                "valid-module",
                "Valid Module",
                "1.0.0",
                "^1.0.0",
                ModuleType.FEATURE,
                "io.govaryn.modules.ValidModule"
            ),
            ModuleDiscoverySource.CLASSPATH,
            "io.govaryn.modules.ValidModule",
            true
        );

        ModuleDiscoveryCandidate invalidCandidate = new ModuleDiscoveryCandidate(
            ModuleMetadata.minimal(
                "invalid-module",
                "Invalid Module",
                "1.0.0",
                "^2.0.0",
                ModuleType.FEATURE,
                "io.govaryn.modules.InvalidModule"
            ),
            ModuleDiscoverySource.CLASSPATH,
            "io.govaryn.modules.InvalidModule",
            true
        );

        ModuleDiscoveryService discoveryService = () -> List.of(validCandidate, invalidCandidate);
        ModuleValidator moduleValidator = (candidates, version) -> List.of(
            new ModuleValidationReport(
                "valid-module",
                "Valid Module",
                ModuleDiscoverySource.CLASSPATH,
                "io.govaryn.modules.ValidModule",
                true,
                List.of()
            ),
            new ModuleValidationReport(
                "invalid-module",
                "Invalid Module",
                ModuleDiscoverySource.CLASSPATH,
                "io.govaryn.modules.InvalidModule",
                false,
                List.of(new ModuleValidationIssue(
                    ModuleValidationSeverity.ERROR,
                    io.govaryn.kernel.module.validation.ModuleValidationCode.KERNEL_API_INCOMPATIBLE,
                    "$.requiredKernelApiVersion",
                    "Incompatible kernel API"
                ))
            )
        );

        InMemoryModuleRegistry registry = new InMemoryModuleRegistry();

        ModuleOrchestrator orchestrator = new ModuleOrchestrator(
            emptyModuleProvider(),
            properties,
            discoveryService,
            moduleValidator,
            registry,
            new ModuleIdentityCollisionDetector(),
            new ModuleDependencyGraphValidator(),
            new ModuleInitializationExecutor(),
            "1.2.0"
        );

        orchestrator.run(new DefaultApplicationArguments(new String[0]));

        assertEquals(1, registry.findAll().size());
        assertTrue(registry.findByModuleId("valid-module").isPresent());
        assertFalse(registry.findByModuleId("invalid-module").isPresent());
        assertEquals(
            ModuleLifecycleState.REGISTERED,
            registry.findByModuleId("valid-module").orElseThrow().status().lifecycleState()
        );
    }

    private static ObjectProvider<KernelModule> emptyModuleProvider() {
        StaticListableBeanFactory factory = new StaticListableBeanFactory();
        return factory.getBeanProvider(KernelModule.class);
    }
}
