package io.govaryn.kernel.module.graph;

import java.util.List;

public class ModuleDependencyCycleException extends IllegalStateException {

    private final List<String> cyclePath;

    public ModuleDependencyCycleException(List<String> cyclePath) {
        super("Module dependency cycle detected: " + String.join(" -> ", cyclePath));
        this.cyclePath = List.copyOf(cyclePath);
    }

    public List<String> cyclePath() {
        return cyclePath;
    }
}
