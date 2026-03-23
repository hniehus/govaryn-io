package io.govaryn.kernel.module;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class ModuleLifecycleTransitions {

    private static final Map<ModuleLifecycleState, Set<ModuleLifecycleState>> ALLOWED_TRANSITIONS;

    static {
        Map<ModuleLifecycleState, Set<ModuleLifecycleState>> transitions = new EnumMap<>(ModuleLifecycleState.class);
        transitions.put(ModuleLifecycleState.DISCOVERED, EnumSet.of(ModuleLifecycleState.VALIDATED, ModuleLifecycleState.REJECTED));
        transitions.put(ModuleLifecycleState.VALIDATED, EnumSet.of(ModuleLifecycleState.REGISTERED));
        transitions.put(ModuleLifecycleState.REJECTED, EnumSet.of(ModuleLifecycleState.DISCOVERED));
        transitions.put(ModuleLifecycleState.REGISTERED, EnumSet.of(ModuleLifecycleState.INITIALIZING));
        transitions.put(ModuleLifecycleState.INITIALIZING, EnumSet.of(
            ModuleLifecycleState.INITIALIZED,
            ModuleLifecycleState.FAILED,
            ModuleLifecycleState.DEGRADED
        ));
        transitions.put(ModuleLifecycleState.INITIALIZED, EnumSet.of(ModuleLifecycleState.FAILED, ModuleLifecycleState.DEGRADED));
        transitions.put(ModuleLifecycleState.DEGRADED, EnumSet.of(ModuleLifecycleState.INITIALIZED, ModuleLifecycleState.FAILED));
        transitions.put(ModuleLifecycleState.FAILED, EnumSet.of(ModuleLifecycleState.REGISTERED));
        ALLOWED_TRANSITIONS = Map.copyOf(transitions);
    }

    private ModuleLifecycleTransitions() {
    }

    public static boolean canTransition(ModuleLifecycleState from, ModuleLifecycleState to) {
        return ALLOWED_TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }
}
