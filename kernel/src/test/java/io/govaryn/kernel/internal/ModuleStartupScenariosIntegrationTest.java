package io.govaryn.kernel.internal;

import io.govaryn.kernel.api.KernelContext;
import io.govaryn.kernel.api.KernelModule;
import io.govaryn.kernel.config.GovarynKernelProperties;
import io.govaryn.kernel.config.KernelEnvironment;
import io.govaryn.kernel.config.ModuleFailurePolicyAction;
import io.govaryn.kernel.config.ModuleMode;
import io.govaryn.kernel.module.InMemoryModuleRegistry;
import io.govaryn.kernel.module.ModuleCapabilities;
import io.govaryn.kernel.module.ModuleFailureDetails;
import io.govaryn.kernel.module.ModuleFailurePolicy;
import io.govaryn.kernel.module.ModuleLifecycleState;
import io.govaryn.kernel.module.ModuleMetadata;
import io.govaryn.kernel.module.ModuleType;
import io.govaryn.kernel.module.discovery.ModuleDiscoveryCandidate;
import io.govaryn.kernel.module.discovery.ModuleDiscoveryService;
import io.govaryn.kernel.module.discovery.ModuleDiscoverySource;
import io.govaryn.kernel.module.graph.ModuleDependencyGraphValidator;
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
        TestKernelModule module = new TestKernelModule("valid-module", false, false);
        InMemoryModuleRegistry registry = new InMemoryModuleRegistry();

        ModuleOrchestrator orchestrator = new ModuleOrchestrator(
            providerFor(module),
            properties,
            () -> List.of(candidate("valid-module", "ValidModule", "^1.0.0", true)),
            new DefaultModuleValidator(),
            registry,
            new ModuleIdentityCollisionDetector(),
            new ModuleDependencyGraphValidator(),
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
        assertTrue(output.getOut().contains("event=module_initialization_succeeded moduleId=valid-module"));
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
            new ModuleDependencyGraphValidator(),
            new ModuleInitializationExecutor(),
            "1.2.0"
        );

        orchestrator.run(new DefaultApplicationArguments(new String[0]));

        assertTrue(registry.findAll().isEmpty());
        assertTrue(output.getOut().contains("errorType=KERNEL_API_INCOMPATIBLE"));
        assertTrue(output.getOut().contains("moduleId 'incompatible-module'"));
        assertTrue(output.getOut().contains("requiredKernelApiVersion '^2.0.0'"));
        assertTrue(output.getOut().contains("running kernel API version '1.2.0'"));
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
            new ModuleDependencyGraphValidator(),
            new ModuleInitializationExecutor(),
            "1.2.0"
        );

        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> orchestrator.run(new DefaultApplicationArguments(new String[0]))
        );
        assertTrue(ex.getMessage().contains("Module identity collision detected"));
        assertTrue(ex.getMessage().contains("moduleId=shared-id"));
        assertTrue(registry.findAll().isEmpty());
    }

    @Test
    @DisplayName("Initialization failure is logged and failure policy is applied")
    void initializationFailureLoggedAndPolicyApplied(CapturedOutput output) {
        GovarynKernelProperties properties = baseProperties(ModuleFailurePolicyAction.REJECT_MODULE_CONTINUE);
        TestKernelModule module = new TestKernelModule("failing-module", true, false);
        InMemoryModuleRegistry registry = new InMemoryModuleRegistry();

        ModuleOrchestrator orchestrator = new ModuleOrchestrator(
            providerFor(module),
            properties,
            () -> List.of(candidate("failing-module", "FailingModule", "^1.0.0", true)),
            new DefaultModuleValidator(),
            registry,
            new ModuleIdentityCollisionDetector(),
            new ModuleDependencyGraphValidator(),
            new ModuleInitializationExecutor(),
            "1.2.0"
        );

        assertDoesNotThrow(() -> orchestrator.run(new DefaultApplicationArguments(new String[0])));
        assertEquals(
            ModuleLifecycleState.FAILED,
            registry.findByModuleId("failing-module").orElseThrow().status().lifecycleState()
        );
        assertTrue(output.getOut().contains("event=module_initialization_failed moduleId=failing-module"));
        assertTrue(output.getOut().contains("moduleVersion=1.0.0"));
        assertTrue(output.getOut().contains("requiredKernelApiVersion=^1.0.0"));
        assertTrue(output.getOut().contains("policy=REJECT_MODULE_CONTINUE"));
    }

    @Test
    @DisplayName("Start failure log contains module context status policy and error cause")
    void startFailureLogContainsStructuredContext(CapturedOutput output) {
        GovarynKernelProperties properties = baseProperties(ModuleFailurePolicyAction.REJECT_MODULE_CONTINUE);
        TestKernelModule module = new TestKernelModule("start-failing-module", false, true);
        InMemoryModuleRegistry registry = new InMemoryModuleRegistry();

        ModuleOrchestrator orchestrator = new ModuleOrchestrator(
            providerFor(module),
            properties,
            () -> List.of(candidate("start-failing-module", "StartFailingModule", "^1.0.0", true)),
            new DefaultModuleValidator(),
            registry,
            new ModuleIdentityCollisionDetector(),
            new ModuleDependencyGraphValidator(),
            new ModuleInitializationExecutor(),
            "1.2.0"
        );

        assertDoesNotThrow(() -> orchestrator.run(new DefaultApplicationArguments(new String[0])));
        assertEquals(
            ModuleLifecycleState.DEGRADED,
            registry.findByModuleId("start-failing-module").orElseThrow().status().lifecycleState()
        );
        assertTrue(output.getOut().contains("event=module_start_failed moduleId=start-failing-module"));
        assertTrue(output.getOut().contains("moduleVersion=1.0.0"));
        assertTrue(output.getOut().contains("currentModuleStatus=DEGRADED"));
        assertTrue(output.getOut().contains("policy=MARK_MODULE_DEGRADED"));
        assertTrue(output.getOut().contains("errorCause=simulated start failure"));
    }

    @Test
    @DisplayName("Start failure falls back to FAILED when MARK_MODULE_DEGRADED transition fails")
    void startFailureFallsBackToFailedWhenMarkDegradedFails(CapturedOutput output) {
        GovarynKernelProperties properties = baseProperties(ModuleFailurePolicyAction.REJECT_MODULE_CONTINUE);
        TestKernelModule module = new TestKernelModule("start-fallback-module", false, true);
        InMemoryModuleRegistry registry = new InMemoryModuleRegistry() {
            @Override
            public void markDegraded(String moduleId, ModuleFailureDetails errorDetails) {
                throw new IllegalStateException("simulated degraded transition failure");
            }
        };

        ModuleOrchestrator orchestrator = new ModuleOrchestrator(
            providerFor(module),
            properties,
            () -> List.of(candidate("start-fallback-module", "StartFallbackModule", "^1.0.0", true)),
            new DefaultModuleValidator(),
            registry,
            new ModuleIdentityCollisionDetector(),
            new ModuleDependencyGraphValidator(),
            new ModuleInitializationExecutor(),
            "1.2.0"
        );

        assertDoesNotThrow(() -> orchestrator.run(new DefaultApplicationArguments(new String[0])));
        assertEquals(
            ModuleLifecycleState.FAILED,
            registry.findByModuleId("start-fallback-module").orElseThrow().status().lifecycleState()
        );
        assertTrue(output.getOut().contains("event=module_start_transition_failed moduleId=start-fallback-module"));
        assertTrue(output.getOut().contains("event=module_start_failed moduleId=start-fallback-module"));
        assertTrue(output.getOut().contains("policy=MARK_MODULE_DEGRADED"));
    }

    @Test
    @DisplayName("Cyclic capability dependencies abort startup with cycle path")
    void cyclicCapabilityDependenciesAbortStartup() {
        GovarynKernelProperties properties = baseProperties(ModuleFailurePolicyAction.REJECT_MODULE_CONTINUE);
        InMemoryModuleRegistry registry = new InMemoryModuleRegistry();

        ModuleDiscoveryService discoveryService = () -> List.of(
            candidateWithCapabilities("mod-a", "ModuleA", List.of("cap.a"), List.of("cap.b")),
            candidateWithCapabilities("mod-b", "ModuleB", List.of("cap.b"), List.of("cap.a"))
        );

        ModuleOrchestrator orchestrator = new ModuleOrchestrator(
            emptyProvider(),
            properties,
            discoveryService,
            new DefaultModuleValidator(),
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

        assertTrue(ex.getMessage().contains("Module dependency cycle detected"));
        assertTrue(ex.getMessage().contains("mod-a"));
        assertTrue(ex.getMessage().contains("mod-b"));
        assertTrue(registry.findAll().isEmpty());
    }

    @Test
    @DisplayName("Unresolved required capabilities abort startup with clear error")
    void unresolvedRequiredCapabilitiesAbortStartup() {
        GovarynKernelProperties properties = baseProperties(ModuleFailurePolicyAction.REJECT_MODULE_CONTINUE);
        InMemoryModuleRegistry registry = new InMemoryModuleRegistry();

        ModuleDiscoveryService discoveryService = () -> List.of(
            candidateWithCapabilities("mod-a", "ModuleA", List.of("cap.a"), List.of("cap.missing"))
        );

        ModuleOrchestrator orchestrator = new ModuleOrchestrator(
            emptyProvider(),
            properties,
            discoveryService,
            new DefaultModuleValidator(),
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

        assertTrue(ex.getMessage().contains("Unresolved required capabilities detected"));
        assertTrue(ex.getMessage().contains("mod-a"));
        assertTrue(ex.getMessage().contains("cap.missing"));
        assertTrue(registry.findAll().isEmpty());
    }

    @Test
    @DisplayName("Module-specific initialization failure policy overrides kernel default")
    void moduleSpecificFailurePolicyOverridesKernelDefault() {
        GovarynKernelProperties properties = baseProperties(ModuleFailurePolicyAction.REJECT_MODULE_CONTINUE);
        TestKernelModule module = new TestKernelModule("module-policy-fail-fast", true, false) {
            @Override
            public ModuleMetadata metadata() {
                return new ModuleMetadata(
                    "1.0.0",
                    "module-policy-fail-fast",
                    "module-policy-fail-fast",
                    "1.0.0",
                    "^1.0.0",
                    ModuleType.FEATURE,
                    getClass().getName(),
                    null,
                    null,
                    null,
                    null,
                    new ModuleCapabilities(List.of("cap.module"), List.of(), List.of()),
                    new ModuleFailurePolicy(ModuleFailurePolicyAction.FAIL_FAST, ModuleFailurePolicyAction.MARK_MODULE_DEGRADED),
                    null,
                    List.of()
                );
            }
        };
        InMemoryModuleRegistry registry = new InMemoryModuleRegistry();

        ModuleOrchestrator orchestrator = new ModuleOrchestrator(
            providerFor(module),
            properties,
            () -> List.of(candidateWithCapabilities("module-policy-fail-fast", "PolicyOverrideModule", List.of("cap.module"), List.of())),
            new DefaultModuleValidator(),
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
        assertEquals(
            ModuleLifecycleState.FAILED,
            registry.findByModuleId("module-policy-fail-fast").orElseThrow().status().lifecycleState()
        );
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

    private static ModuleDiscoveryCandidate candidateWithCapabilities(
        String moduleId,
        String moduleName,
        List<String> providedCapabilities,
        List<String> requiredCapabilities
    ) {
        return new ModuleDiscoveryCandidate(
            new ModuleMetadata(
                "1.0.0",
                moduleId,
                moduleName,
                "1.0.0",
                "^1.0.0",
                ModuleType.FEATURE,
                "io.govaryn.modules." + moduleName,
                null,
                null,
                null,
                null,
                new ModuleCapabilities(providedCapabilities, requiredCapabilities, List.of()),
                null,
                null,
                List.of()
            ),
            ModuleDiscoverySource.CLASSPATH,
            "io.govaryn.modules." + moduleName,
            true
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
        private final boolean failOnStart;

        private TestKernelModule(String moduleId, boolean failOnInitialize, boolean failOnStart) {
            this.moduleId = moduleId;
            this.failOnInitialize = failOnInitialize;
            this.failOnStart = failOnStart;
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
            if (failOnStart) {
                throw new IllegalStateException("simulated start failure");
            }
        }
    }
}
