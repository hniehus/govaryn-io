package io.govaryn.kernel.module.graph;

import io.govaryn.kernel.module.ModuleCapabilities;
import io.govaryn.kernel.module.ModuleMetadata;
import io.govaryn.kernel.module.ModuleType;
import io.govaryn.kernel.module.discovery.ModuleDiscoveryCandidate;
import io.govaryn.kernel.module.discovery.ModuleDiscoverySource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Module Dependency Graph Validator Tests")
class ModuleDependencyGraphValidatorTest {

    private final ModuleDependencyGraphValidator validator = new ModuleDependencyGraphValidator();

    @Test
    @DisplayName("Should derive acyclic graph from capability dependencies")
    void shouldDeriveAcyclicGraph() {
        List<ModuleDiscoveryCandidate> candidates = List.of(
            candidate("mod-a", "ModuleA", List.of("cap.a"), List.of("cap.b")),
            candidate("mod-b", "ModuleB", List.of("cap.b"), List.of("cap.c")),
            candidate("mod-c", "ModuleC", List.of("cap.c"), List.of())
        );

        ModuleDependencyGraph graph = validator.deriveGraph(candidates);
        Optional<List<String>> cycle = validator.detectCycle(graph);

        assertTrue(cycle.isEmpty());
        assertEquals(List.of("mod-b"), graph.dependenciesByModuleId().get("mod-a"));
        assertEquals(List.of("mod-c"), graph.dependenciesByModuleId().get("mod-b"));
        assertEquals(List.of(), graph.dependenciesByModuleId().get("mod-c"));
    }

    @Test
    @DisplayName("Should detect cycle and include cycle path")
    void shouldDetectCycleWithPath() {
        List<ModuleDiscoveryCandidate> candidates = List.of(
            candidate("mod-a", "ModuleA", List.of("cap.a"), List.of("cap.b")),
            candidate("mod-b", "ModuleB", List.of("cap.b"), List.of("cap.a"))
        );

        ModuleDependencyGraph graph = validator.deriveGraph(candidates);

        ModuleDependencyCycleException ex = assertThrows(
            ModuleDependencyCycleException.class,
            () -> validator.assertAcyclic(graph)
        );

        assertTrue(ex.getMessage().contains("Module dependency cycle detected"));
        assertTrue(ex.getMessage().contains("mod-a"));
        assertTrue(ex.getMessage().contains("mod-b"));
        assertTrue(ex.cyclePath().size() >= 3);
    }

    private static ModuleDiscoveryCandidate candidate(
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
}
