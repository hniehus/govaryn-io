package io.govaryn.kernel.internal;

import io.govaryn.kernel.api.KernelContext;
import io.govaryn.kernel.api.KernelModule;
import io.govaryn.kernel.config.GovarynKernelProperties;
import io.govaryn.kernel.config.KernelEnvironment;
import io.govaryn.kernel.config.ModuleFailurePolicyAction;
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
import io.govaryn.kernel.module.validation.ModuleValidationReport;
import io.govaryn.kernel.module.validation.ModuleValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Module Initialization Failure Policy Tests")
@ExtendWith(OutputCaptureExtension.class)
class ModuleInitializationFailurePolicyTest {

    @Test
    @DisplayName("Should continue startup when module fails during initialization and policy is REJECT_MODULE_CONTINUE")
    void shouldContinueOnInitializationFailureWhenPolicyRejectContinue(CapturedOutput output) {
        TestKernelModule failing = new TestKernelModule("failing-module", true);
        TestKernelModule healthy = new TestKernelModule("healthy-module", false);

        GovarynKernelProperties properties = baseProperties(ModuleFailurePolicyAction.REJECT_MODULE_CONTINUE);
        ModuleDiscoveryService discoveryService = () -> List.of(
            discoveryCandidate("failing-module", "io.govaryn.modules.FailingModule"),
            discoveryCandidate("healthy-module", "io.govaryn.modules.HealthyModule")
        );
        ModuleValidator validator = allValidValidator();
        InMemoryModuleRegistry registry = new InMemoryModuleRegistry();

        ModuleOrchestrator orchestrator = new ModuleOrchestrator(
            providerFor(failing, healthy),
            properties,
            discoveryService,
            validator,
            registry,
            new ModuleIdentityCollisionDetector(),
            new ModuleDependencyGraphValidator(),
            new ModuleInitializationExecutor(),
            "1.2.0"
        );

        assertDoesNotThrow(() -> orchestrator.run(new DefaultApplicationArguments(new String[0])));

        assertEquals(ModuleLifecycleState.FAILED, registry.findByModuleId("failing-module").orElseThrow().status().lifecycleState());
        assertEquals(ModuleLifecycleState.INITIALIZED, registry.findByModuleId("healthy-module").orElseThrow().status().lifecycleState());
        assertFalse(failing.started);
        assertTrue(healthy.started);
        assertTrue(output.getOut().contains("event=module_initialization_failed moduleId=failing-module"));
        assertTrue(output.getOut().contains("policy=REJECT_MODULE_CONTINUE"));
    }

    @Test
    @DisplayName("Should fail fast when module fails during initialization and policy is FAIL_FAST")
    void shouldFailFastOnInitializationFailure(CapturedOutput output) {
        TestKernelModule failing = new TestKernelModule("failing-module", true);

        GovarynKernelProperties properties = baseProperties(ModuleFailurePolicyAction.FAIL_FAST);
        ModuleDiscoveryService discoveryService = () -> List.of(
            discoveryCandidate("failing-module", "io.govaryn.modules.FailingModule")
        );
        ModuleValidator validator = allValidValidator();
        InMemoryModuleRegistry registry = new InMemoryModuleRegistry();

        ModuleOrchestrator orchestrator = new ModuleOrchestrator(
            providerFor(failing),
            properties,
            discoveryService,
            validator,
            registry,
            new ModuleIdentityCollisionDetector(),
            new ModuleDependencyGraphValidator(),
            new ModuleInitializationExecutor(),
            "1.2.0"
        );

        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> orchestrator.run(new DefaultApplicationArguments(new String[0]))
        );

        assertTrue(ex.getMessage().contains("FAIL_FAST"));
        assertEquals(ModuleLifecycleState.FAILED, registry.findByModuleId("failing-module").orElseThrow().status().lifecycleState());
        assertTrue(output.getOut().contains("event=module_initialization_failed moduleId=failing-module"));
        assertTrue(output.getOut().contains("policy=FAIL_FAST"));
    }

    @Test
    @DisplayName("Should mark module as degraded when initialization fails and policy is MARK_MODULE_DEGRADED")
    void shouldMarkDegradedOnInitializationFailureWhenPolicyConfigured(CapturedOutput output) {
        TestKernelModule failing = new TestKernelModule("failing-module", true);

        GovarynKernelProperties properties = baseProperties(ModuleFailurePolicyAction.MARK_MODULE_DEGRADED);
        ModuleDiscoveryService discoveryService = () -> List.of(
            discoveryCandidate("failing-module", "io.govaryn.modules.FailingModule")
        );
        ModuleValidator validator = allValidValidator();
        InMemoryModuleRegistry registry = new InMemoryModuleRegistry();

        ModuleOrchestrator orchestrator = new ModuleOrchestrator(
            providerFor(failing),
            properties,
            discoveryService,
            validator,
            registry,
            new ModuleIdentityCollisionDetector(),
            new ModuleDependencyGraphValidator(),
            new ModuleInitializationExecutor(),
            "1.2.0"
        );

        assertDoesNotThrow(() -> orchestrator.run(new DefaultApplicationArguments(new String[0])));
        assertEquals(ModuleLifecycleState.DEGRADED, registry.findByModuleId("failing-module").orElseThrow().status().lifecycleState());
        assertTrue(registry.findByModuleId("failing-module").orElseThrow().status().degraded());
        assertTrue(output.getOut().contains("event=module_initialization_failed moduleId=failing-module"));
        assertTrue(output.getOut().contains("policy=MARK_MODULE_DEGRADED"));
    }

    private static GovarynKernelProperties baseProperties(ModuleFailurePolicyAction policy) {
        GovarynKernelProperties properties = new GovarynKernelProperties();
        properties.setId("test-kernel");
        properties.setEnvironment(KernelEnvironment.DEV);
        properties.setModuleMode(ModuleMode.CLASSPATH);
        properties.setModulePluginDirectory("./plugins");
        properties.setModuleInitializationFailurePolicy(policy);
        return properties;
    }

    private static ModuleDiscoveryCandidate discoveryCandidate(String moduleId, String origin) {
        return new ModuleDiscoveryCandidate(
            ModuleMetadata.minimal(
                moduleId,
                moduleId,
                "1.0.0",
                "^1.0.0",
                ModuleType.FEATURE,
                origin
            ),
            ModuleDiscoverySource.CLASSPATH,
            origin,
            true
        );
    }

    private static ModuleValidator allValidValidator() {
        return (candidates, version) -> candidates.stream()
            .map(c -> new ModuleValidationReport(
                c.metadata().moduleId(),
                c.metadata().moduleName(),
                c.source(),
                c.origin(),
                true,
                List.of()
            ))
            .toList();
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
        private final String moduleId;
        private final boolean failOnInitialize;
        private boolean started;

        private TestKernelModule(String moduleId, boolean failOnInitialize) {
            this.moduleId = moduleId;
            this.failOnInitialize = failOnInitialize;
        }

        @Override
        public int order() {
            return "failing-module".equals(moduleId) ? 1 : 2;
        }

        @Override
        public ModuleMetadata metadata() {
            return ModuleMetadata.minimal(
                moduleId,
                moduleId,
                "1.0.0",
                "^1.0.0",
                ModuleType.FEATURE,
                getClass().getName()
            );
        }

        @Override
        public void initialize(KernelContext context) {
            if (failOnInitialize) {
                throw new IllegalStateException("simulated init failure");
            }
        }

        @Override
        public void start() {
            started = true;
        }
    }
}
