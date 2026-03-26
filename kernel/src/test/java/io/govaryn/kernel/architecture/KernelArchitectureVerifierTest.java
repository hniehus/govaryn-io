package io.govaryn.kernel.architecture;

import io.govaryn.kernel.api.KernelModule;
import io.govaryn.kernel.module.discovery.ModuleDiscoveryCandidate;
import io.govaryn.kernel.module.discovery.ModuleDiscoverySource;
import io.govaryn.kernel.module.graph.ModuleDependencyGraph;
import io.govaryn.kernel.module.graph.ModuleDependencyGraphValidator;
import io.govaryn.kernel.module.validation.DefaultModuleValidator;
import io.govaryn.kernel.module.validation.ModuleValidationReport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@DisplayName("Kernel Architecture Verifier")
class KernelArchitectureVerifierTest {

    private static final String RUNNING_KERNEL_API_VERSION = "1.2.0";
    private static final String MODULES_BASE_PACKAGE = "io.govaryn.modules.examples";
    private static final Path MODULES_SOURCE_ROOT = Path.of("..", "modules", "src", "main", "java");
    private static final Path FORBIDDEN_API_SAMPLE = Path.of(
        "..",
        "docs",
        "modules",
        "negative-examples",
        "ForbiddenInternalApiUsageModule.java.sample"
    );

    /**
     * Intentional negative reference modules used by scenario tests.
     * They are excluded from conformance verification by design.
     */
    private static final Set<String> EXCLUDED_REFERENCE_FIXTURES = Set.of(
        "io.govaryn.modules.examples.IncompatibleApiVersionModule",
        "io.govaryn.modules.examples.DuplicateIdModuleA",
        "io.govaryn.modules.examples.DuplicateIdModuleB",
        "io.govaryn.modules.examples.FailingInitializationModule",
        "io.govaryn.modules.examples.MissingRequiredFieldModule"
    );

    @Test
    @DisplayName("Module contract conformance is enforced for reference modules")
    void moduleContractConformanceIsEnforced() {
        List<ModuleDiscoveryCandidate> candidates = loadVerifiableModuleCandidates();
        List<ModuleValidationReport> reports = new DefaultModuleValidator().validate(candidates, RUNNING_KERNEL_API_VERSION);

        List<String> violations = reports.stream()
            .filter(report -> !report.valid())
            .map(report -> report.moduleId() + " -> " + report.issues())
            .toList();

        assertFalse(candidates.isEmpty(), "Architecture verifier did not find any verifiable modules");
        assertTrue(violations.isEmpty(), "Module contract violations detected: " + violations);
    }

    @Test
    @DisplayName("Modules must not use kernel internal APIs")
    void internalApiUsageIsForbiddenForModules() throws Exception {
        if (!Files.exists(MODULES_SOURCE_ROOT)) {
            fail("Modules source root does not exist: " + MODULES_SOURCE_ROOT.toAbsolutePath());
            return;
        }

        List<String> violations = new ArrayList<>();
        try (var paths = Files.walk(MODULES_SOURCE_ROOT)) {
            paths.filter(path -> path.toString().endsWith(".java"))
                .forEach(path -> {
                    try {
                        String content = Files.readString(path);
                        if (containsForbiddenInternalApiUsage(content)) {
                            violations.add(path.toAbsolutePath().toString());
                        }
                    } catch (Exception ex) {
                        violations.add(path.toAbsolutePath() + " (read error: " + ex.getMessage() + ")");
                    }
                });
        }

        assertTrue(violations.isEmpty(), "Forbidden internal API usage detected in module sources: " + violations);
    }

    @Test
    @DisplayName("Forbidden internal API sample is detected by verifier rule")
    void forbiddenInternalApiSampleIsDetected() throws Exception {
        assertTrue(Files.exists(FORBIDDEN_API_SAMPLE), "Sample file missing: " + FORBIDDEN_API_SAMPLE.toAbsolutePath());
        String sample = Files.readString(FORBIDDEN_API_SAMPLE);
        assertTrue(
            containsForbiddenInternalApiUsage(sample),
            "Verifier must flag internal API usage in the negative sample"
        );
    }

    @Test
    @DisplayName("Dependency graph for verifiable modules must be acyclic")
    void dependencyGraphMustBeAcyclic() {
        List<ModuleDiscoveryCandidate> candidates = loadVerifiableModuleCandidates();
        ModuleDependencyGraphValidator validator = new ModuleDependencyGraphValidator();
        ModuleDependencyGraph graph = validator.deriveGraph(candidates);

        assertTrue(
            validator.detectCycle(graph).isEmpty(),
            () -> "Forbidden dependency cycle detected: " + validator.detectCycle(graph).orElse(List.of())
        );
    }

    private static List<ModuleDiscoveryCandidate> loadVerifiableModuleCandidates() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(KernelModule.class));

        Set<String> classNames = new LinkedHashSet<>();
        scanner.findCandidateComponents(MODULES_BASE_PACKAGE)
            .forEach(component -> classNames.add(component.getBeanClassName()));

        List<ModuleDiscoveryCandidate> candidates = new ArrayList<>();
        for (String className : classNames) {
            if (className == null || EXCLUDED_REFERENCE_FIXTURES.contains(className)) {
                continue;
            }

            try {
                Class<?> rawClass = Class.forName(className);
                KernelModule module = (KernelModule) rawClass.getDeclaredConstructor().newInstance();
                candidates.add(new ModuleDiscoveryCandidate(
                    module.metadata(),
                    ModuleDiscoverySource.CLASSPATH,
                    className,
                    true
                ));
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to instantiate module for verification: " + className, ex);
            }
        }
        return List.copyOf(candidates);
    }

    private static boolean containsForbiddenInternalApiUsage(String content) {
        return content.contains("io.govaryn.kernel.internal.");
    }
}
