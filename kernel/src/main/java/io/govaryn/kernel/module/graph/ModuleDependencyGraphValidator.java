package io.govaryn.kernel.module.graph;

import io.govaryn.kernel.module.ModuleCapabilities;
import io.govaryn.kernel.module.discovery.ModuleDiscoveryCandidate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class ModuleDependencyGraphValidator {

    public ModuleDependencyGraph deriveGraph(List<ModuleDiscoveryCandidate> candidates) {
        return validateGraph(candidates).graph();
    }

    public ModuleDependencyGraphValidation validateGraph(List<ModuleDiscoveryCandidate> candidates) {
        Map<String, Set<String>> providersByCapability = new LinkedHashMap<>();
        Map<String, Set<String>> dependenciesByModuleId = new LinkedHashMap<>();
        Map<String, List<String>> unresolvedRequiredCapabilitiesByModuleId = new LinkedHashMap<>();

        for (ModuleDiscoveryCandidate candidate : candidates) {
            String moduleId = candidate.metadata().moduleId();
            dependenciesByModuleId.putIfAbsent(moduleId, new LinkedHashSet<>());

            ModuleCapabilities capabilities = candidate.metadata().capabilities();
            for (String providedCapability : capabilities.providedCapabilities()) {
                providersByCapability.computeIfAbsent(providedCapability.trim(), ignored -> new LinkedHashSet<>())
                    .add(moduleId);
            }
        }

        for (ModuleDiscoveryCandidate candidate : candidates) {
            String moduleId = candidate.metadata().moduleId();
            Set<String> dependencies = dependenciesByModuleId.get(moduleId);
            List<String> unresolved = new ArrayList<>();

            for (String requiredCapability : candidate.metadata().capabilities().requiredCapabilities()) {
                Set<String> providers = providersByCapability.getOrDefault(requiredCapability.trim(), Set.of());
                if (providers.isEmpty()) {
                    unresolved.add(requiredCapability.trim());
                } else {
                    dependencies.addAll(providers);
                }
            }

            if (!unresolved.isEmpty()) {
                unresolvedRequiredCapabilitiesByModuleId.put(moduleId, List.copyOf(unresolved));
            }
        }

        Map<String, List<String>> immutableDependencies = new LinkedHashMap<>();
        dependenciesByModuleId.forEach((moduleId, dependencies) ->
            immutableDependencies.put(moduleId, List.copyOf(dependencies))
        );

        return new ModuleDependencyGraphValidation(
            new ModuleDependencyGraph(immutableDependencies),
            Map.copyOf(unresolvedRequiredCapabilitiesByModuleId)
        );
    }

    public Optional<List<String>> detectCycle(ModuleDependencyGraph graph) {
        Map<String, VisitState> states = new LinkedHashMap<>();
        List<String> currentPath = new ArrayList<>();
        Map<String, Integer> currentPathIndex = new LinkedHashMap<>();

        for (String moduleId : graph.dependenciesByModuleId().keySet()) {
            states.putIfAbsent(moduleId, VisitState.UNVISITED);
        }

        for (String moduleId : graph.dependenciesByModuleId().keySet()) {
            if (states.get(moduleId) == VisitState.UNVISITED) {
                Optional<List<String>> cycle = dfs(moduleId, graph, states, currentPath, currentPathIndex);
                if (cycle.isPresent()) {
                    return cycle;
                }
            }
        }

        return Optional.empty();
    }

    public void assertAcyclic(ModuleDependencyGraph graph) {
        detectCycle(graph).ifPresent(cyclePath -> {
            throw new ModuleDependencyCycleException(cyclePath);
        });
    }

    private Optional<List<String>> dfs(
        String moduleId,
        ModuleDependencyGraph graph,
        Map<String, VisitState> states,
        List<String> currentPath,
        Map<String, Integer> currentPathIndex
    ) {
        states.put(moduleId, VisitState.VISITING);
        currentPathIndex.put(moduleId, currentPath.size());
        currentPath.add(moduleId);

        for (String dependency : graph.dependenciesByModuleId().getOrDefault(moduleId, List.of())) {
            VisitState dependencyState = states.getOrDefault(dependency, VisitState.UNVISITED);
            if (dependencyState == VisitState.VISITING) {
                return Optional.of(resolveCyclePath(dependency, currentPath, currentPathIndex));
            }
            if (dependencyState == VisitState.UNVISITED) {
                Optional<List<String>> nestedCycle = dfs(dependency, graph, states, currentPath, currentPathIndex);
                if (nestedCycle.isPresent()) {
                    return nestedCycle;
                }
            }
        }

        currentPath.remove(currentPath.size() - 1);
        currentPathIndex.remove(moduleId);
        states.put(moduleId, VisitState.VISITED);
        return Optional.empty();
    }

    private static List<String> resolveCyclePath(
        String cycleStart,
        List<String> currentPath,
        Map<String, Integer> currentPathIndex
    ) {
        int startIndex = currentPathIndex.get(cycleStart);
        List<String> cycle = new ArrayList<>(currentPath.subList(startIndex, currentPath.size()));
        cycle.add(cycleStart);
        return List.copyOf(cycle);
    }

    private enum VisitState {
        UNVISITED,
        VISITING,
        VISITED
    }

    public record ModuleDependencyGraphValidation(
        ModuleDependencyGraph graph,
        Map<String, List<String>> unresolvedRequiredCapabilitiesByModuleId
    ) {
    }
}
