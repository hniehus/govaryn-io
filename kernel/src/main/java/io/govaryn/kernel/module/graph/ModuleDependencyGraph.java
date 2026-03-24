package io.govaryn.kernel.module.graph;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

public record ModuleDependencyGraph(Map<String, List<String>> dependenciesByModuleId) {

    public ModuleDependencyGraph {
        Map<String, List<String>> normalized = new LinkedHashMap<>();
        dependenciesByModuleId.forEach((moduleId, dependencies) ->
            normalized.put(moduleId, dependencies == null ? List.of() : List.copyOf(dependencies))
        );
        dependenciesByModuleId = Collections.unmodifiableMap(normalized);
    }
}
