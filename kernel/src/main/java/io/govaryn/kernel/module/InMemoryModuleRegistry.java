package io.govaryn.kernel.module;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryModuleRegistry implements ModuleRegistry {

    private final Map<String, ModuleRegistryEntry> entries = new ConcurrentHashMap<>();

    @Override
    public void addDiscovered(ModuleMetadata metadata, boolean mandatory, String origin) {
        ModuleRegistryEntry entry = new ModuleRegistryEntry(metadata, ModuleStatus.discovered(), mandatory, origin);
        ModuleRegistryEntry previous = entries.putIfAbsent(metadata.moduleId(), entry);
        if (previous != null) {
            throw new IllegalStateException(
                "Duplicate moduleId detected: " + metadata.moduleId()
                    + " (existing: " + previous.metadata().moduleName()
                    + "@" + previous.origin()
                    + ", incoming: " + metadata.moduleName()
                    + "@" + origin + ")"
            );
        }
    }

    @Override
    public void registerValidated(ModuleMetadata metadata, boolean mandatory, String origin, String runningKernelApiVersion) {
        ensureKernelApiCompatibility(metadata, runningKernelApiVersion);
        addDiscovered(metadata, mandatory, origin);
        transitionState(metadata.moduleId(), ModuleLifecycleState.VALIDATED, null);
        transitionState(metadata.moduleId(), ModuleLifecycleState.REGISTERED, null);
    }

    @Override
    public Optional<ModuleRegistryEntry> findByModuleId(String moduleId) {
        return Optional.ofNullable(entries.get(moduleId));
    }

    @Override
    public List<ModuleRegistryEntry> findAll() {
        return entries.values().stream()
            .sorted(Comparator.comparing(entry -> entry.metadata().moduleId()))
            .toList();
    }

    @Override
    public void transitionState(String moduleId, ModuleLifecycleState targetState, ModuleFailureDetails errorDetails) {
        entries.compute(moduleId, (id, current) -> {
            if (current == null) {
                throw new IllegalArgumentException("Unknown moduleId: " + moduleId);
            }

            ModuleLifecycleState sourceState = current.status().lifecycleState();
            if (!ModuleLifecycleTransitions.canTransition(sourceState, targetState)) {
                throw new IllegalStateException("Invalid state transition: " + sourceState + " -> " + targetState);
            }

            return current.withStatus(ModuleStatus.withState(targetState, errorDetails));
        });
    }

    @Override
    public void markDegraded(String moduleId, ModuleFailureDetails errorDetails) {
        entries.compute(moduleId, (id, current) -> {
            if (current == null) {
                throw new IllegalArgumentException("Unknown moduleId: " + moduleId);
            }
            ModuleLifecycleState state = current.status().lifecycleState();
            if (state != ModuleLifecycleState.INITIALIZING && state != ModuleLifecycleState.INITIALIZED) {
                throw new IllegalStateException("Degraded mode only allowed for initializing or initialized modules");
            }
            if (!ModuleLifecycleTransitions.canTransition(state, ModuleLifecycleState.DEGRADED)) {
                throw new IllegalStateException("Invalid state transition: " + state + " -> " + ModuleLifecycleState.DEGRADED);
            }
            return current.withStatus(ModuleStatus.degraded(errorDetails));
        });
    }

    @Override
    public void remove(String moduleId) {
        entries.remove(moduleId);
    }

    private static void ensureKernelApiCompatibility(ModuleMetadata metadata, String runningKernelApiVersion) {
        KernelApiVersionCompatibility.validateRunningKernelApiVersion(runningKernelApiVersion);
        if (!KernelApiVersionCompatibility.isCompatible(metadata.requiredKernelApiVersion(), runningKernelApiVersion)) {
            throw new IllegalStateException(
                "Cannot register moduleId '" + metadata.moduleId()
                    + "': requiredKernelApiVersion '" + metadata.requiredKernelApiVersion()
                    + "' is incompatible with running kernel API version '" + runningKernelApiVersion + "'"
            );
        }
    }
}
