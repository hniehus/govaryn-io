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
import io.govaryn.kernel.module.identity.ModuleIdentityCollisionDetector;
import io.govaryn.kernel.module.validation.DefaultModuleValidator;
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

@ExtendWith(OutputCaptureExtension.class)
@DisplayName("Module Startup Scenarios Integration Tests")
class ModuleStartupScenariosIntegrationTest {

    @Test
    @DisplayName("Valid module is discovered validated and registered")
    void validModuleDiscoveredValidatedRegistered(CapturedOutput output) {
        GovarynKernelProperties properties = baseProperties(ModuleFailurePolicyAction.REJECT_MODULE_CONTINUE);
        TestKernelModule module = new TestKernelModule("valid-module", false);
        InMemoryModuleRegistry registry = new InMemoryModuleRegistry();

        ModuleOrchestrator orchestrator = new ModuleOrchestrator(
            providerFor(module),
            properties,
            () -> List.of(candidate("valid-module", "ValidModule", "^1.0.0", true)),
            new DefaultModuleValidator(),
            registry,
            new ModuleIdentityCollisionDetector(),
            new ModuleInitializationExecutor(),
            "1.2.0"
        );

        orchestrator.run(new DefaultApplicationArguments(new String[0]));

        assertTrue(registry.findByModuleId("valid-module").isPresent());
        assertEquals(
            ModuleLifecycleState.INITIALIZED,
            registry.findByModuleId("valid-module").orElseThrow().status().lifecycleState()
        );
        assertTrue(output.getOut().contains("event=module_discovered moduleId=valid-module"));
        assertTrue(output.getOut().contains("event=module_validation_report moduleId=valid-module"));
        assertTrue(output.getOut().contains("event=module_registered moduleId=valid-module"));
    }

    @Test
    @DisplayName("Incompatible API version is rejected")
    void incompatibleApiVersionRejected(CapturedOutput output) {
        GovarynKernelProperties properties = baseProperties(ModuleFailurePolicyAction.REJECT_MODULE_CONTINUE);
        InMemoryModuleRegistry registry = new InMemoryModuleRegistry();

        ModuleOrchestrator orchestrator = new ModuleOrchestrator(
            emptyProvider(),
            properties,
            () -> List.of(candidate("incompatible-module", "IncompatibleModule", "^2.0.0", false)),
            new DefaultModuleValidator(),
            registry,
            new ModuleIdentityCollisionDetector(),
            new ModuleInitializationExecutor(),
            "1.2.0"
        );

        orchestrator.run(new DefaultApplicationArguments(new String[0]));

        assertTrue(registry.findAll().isEmpty());
        assertTrue(output.getOut().contains("errorType=KERNEL_API_INCOMPATIBLE"));
        assertTrue(output.getOut().contains("event=module_startup_summary found=1 validated=0 rejected=1 registered=0 failed=0 degraded=0"));
    }

    @Test
    @DisplayName("Duplicate moduleId leads to startup error")
    void duplicateModuleIdFailsStartup() {
        GovarynKernelProperties properties = baseProperties(ModuleFailurePolicyAction.REJECT_MODULE_CONTINUE);
        InMemoryModuleRegistry registry = new InMemoryModuleRegistry();

        ModuleOrchestrator orchestrator = new ModuleOrchestrator(
            emptyProvider(),
            properties,
            () -> List.of(
                candidate("shared-id", "ClasspathModule", "^1.0.0", true),
                new ModuleDiscoveryCandidate(
                    ModuleMetadata.minimal(
                        "shared-id",
                        "PluginModule",
                        "1.0.0",
                        "^1.0.0",
                        ModuleType.FEATURE,
                        "io.govaryn.plugins.PluginModule"
                    ),
                    ModuleDiscoverySource.MANIFEST_SCAN,
                    "/plugins/plugin/module.json",
                    false
                )
            ),
            new DefaultModuleValidator(),
            registry,
            new ModuleIdentityCollisionDetector(),
            new ModuleInitializationExecutor(),
            "1.2.0"
        );

        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> orchestrator.run(new DefaultApplicationArguments(new String[0]))
        );
        assertTrue(ex.getMessage().contains("Module identity collision detected"));
        assertTrue(ex.getMessage().contains("shared-id"));
    }

    @Test
    @DisplayName("Initialization failure is logged and failure policy is applied")
    void initializationFailureLoggedAndPolicyApplied(CapturedOutput output) {
        GovarynKernelProperties properties = baseProperties(ModuleFailurePolicyAction.REJECT_MODULE_CONTINUE);
        TestKernelModule module = new TestKernelModule("failing-module", true);
        InMemoryModuleRegistry registry = new InMemoryModuleRegistry();

        ModuleOrchestrator orchestrator = new ModuleOrchestrator(
            providerFor(module),
            properties,
            () -> List.of(candidate("failing-module", "FailingModule", "^1.0.0", true)),
            new DefaultModuleValidator(),
            registry,
            new ModuleIdentityCollisionDetector(),
            new ModuleInitializationExecutor(),
            "1.2.0"
        );

        assertDoesNotThrow(() -> orchestrator.run(new DefaultApplicationArguments(new String[0])));
        assertEquals(
            ModuleLifecycleState.FAILED,
            registry.findByModuleId("failing-module").orElseThrow().status().lifecycleState()
        );
        assertTrue(output.getOut().contains("event=module_initialization_failed moduleId=failing-module"));
        assertTrue(output.getOut().contains("policy=REJECT_MODULE_CONTINUE"));
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

    private static ModuleDiscoveryCandidate candidate(String moduleId, String moduleName, String requiredKernelApiVersion, boolean loadable) {
        return new ModuleDiscoveryCandidate(
            ModuleMetadata.minimal(
                moduleId,
                moduleName,
                "1.0.0",
                requiredKernelApiVersion,
                ModuleType.FEATURE,
                "io.govaryn.modules." + moduleName
            ),
            ModuleDiscoverySource.CLASSPATH,
            "io.govaryn.modules." + moduleName,
            loadable
        );
    }

    @SafeVarargs
    private static ObjectProvider<KernelModule> providerFor(KernelModule... modules) {
        StaticListableBeanFactory factory = new StaticListableBeanFactory();
        for (int i = 0; i < modules.length; i++) {
            factory.addBean("module" + i, modules[i]);
        }
        return factory.getBeanProvider(KernelModule.class);
    }

    private static ObjectProvider<KernelModule> emptyProvider() {
        StaticListableBeanFactory factory = new StaticListableBeanFactory();
        return factory.getBeanProvider(KernelModule.class);
    }

    private static class TestKernelModule implements KernelModule {
        private final String moduleId;
        private final boolean failOnInitialize;

        private TestKernelModule(String moduleId, boolean failOnInitialize) {
            this.moduleId = moduleId;
            this.failOnInitialize = failOnInitialize;
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
    }
}
