package io.govaryn.kernel.health;

import io.govaryn.kernel.module.ModuleRegistry;
import io.govaryn.kernel.module.ModuleRegistryEntry;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ModuleStatusService {

    private final ModuleRegistry moduleRegistry;

    public ModuleStatusService(ModuleRegistry moduleRegistry) {
        this.moduleRegistry = moduleRegistry;
    }

    public ModuleStatusResponse currentStatus() {
        List<ModuleStatusItemResponse> modules = moduleRegistry.findAll().stream()
            .map(ModuleStatusService::mapToStatusItem)
            .toList();

        return new ModuleStatusResponse(modules);
    }

    private static ModuleStatusItemResponse mapToStatusItem(ModuleRegistryEntry entry) {
        String errorCause = entry.status().lastError() == null ? null : entry.status().lastError().message();
        return new ModuleStatusItemResponse(
            entry.metadata().moduleId(),
            entry.metadata().moduleVersion(),
            entry.status().lifecycleState().name(),
            entry.status().degraded(),
            entry.metadata().failurePolicy().onInitializationFailure().name(),
            errorCause
        );
    }
}
