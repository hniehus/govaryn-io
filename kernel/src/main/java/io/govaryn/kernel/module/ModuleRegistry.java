package io.govaryn.kernel.module;

import java.util.List;
import java.util.Optional;

public interface ModuleRegistry {

    default void addDiscovered(ModuleMetadata metadata, boolean mandatory) {
        addDiscovered(metadata, mandatory, "unknown");
    }

    void addDiscovered(ModuleMetadata metadata, boolean mandatory, String origin);

    default void registerValidated(ModuleMetadata metadata, boolean mandatory, String origin) {
        addDiscovered(metadata, mandatory, origin);
        transitionState(metadata.moduleId(), ModuleLifecycleState.VALIDATED, null);
        transitionState(metadata.moduleId(), ModuleLifecycleState.REGISTERED, null);
    }

    Optional<ModuleRegistryEntry> findByModuleId(String moduleId);

    List<ModuleRegistryEntry> findAll();

    void transitionState(String moduleId, ModuleLifecycleState targetState, ModuleFailureDetails errorDetails);

    void markDegraded(String moduleId, ModuleFailureDetails errorDetails);

    void remove(String moduleId);
}
