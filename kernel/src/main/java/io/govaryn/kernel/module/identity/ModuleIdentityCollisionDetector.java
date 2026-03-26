package io.govaryn.kernel.module.identity;

import io.govaryn.kernel.module.ModuleRegistry;
import io.govaryn.kernel.module.ModuleRegistryEntry;
import io.govaryn.kernel.module.discovery.ModuleDiscoveryCandidate;
import io.govaryn.kernel.module.discovery.ModuleDiscoverySource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ModuleIdentityCollisionDetector {

    public List<ModuleIdentityCollision> detect(List<ModuleDiscoveryCandidate> candidates, ModuleRegistry registry) {
        Map<String, List<ModuleIdentityReference>> byModuleId = new LinkedHashMap<>();

        for (ModuleDiscoveryCandidate candidate : candidates) {
            byModuleId.computeIfAbsent(candidate.metadata().moduleId(), id -> new ArrayList<>())
                .add(new ModuleIdentityReference(
                    candidate.metadata().moduleId(),
                    candidate.metadata().moduleName(),
                    candidate.metadata().moduleVersion(),
                    candidate.source(),
                    candidate.origin()
                ));

            Optional<ModuleRegistryEntry> existing = registry.findByModuleId(candidate.metadata().moduleId());
            existing.ifPresent(entry ->
                byModuleId.get(candidate.metadata().moduleId()).add(new ModuleIdentityReference(
                    entry.metadata().moduleId(),
                    entry.metadata().moduleName(),
                    entry.metadata().moduleVersion(),
                    ModuleDiscoverySource.PLUGIN_DIRECTORY,
                    entry.origin()
                ))
            );
        }

        return byModuleId.entrySet().stream()
            .filter(entry -> entry.getValue().size() > 1)
            .map(entry -> new ModuleIdentityCollision(entry.getKey(), entry.getValue()))
            .toList();
    }

    public String formatCollisionMessage(List<ModuleIdentityCollision> collisions) {
        String details = collisions.stream()
            .map(collision -> {
                String refs = collision.references().stream()
                    .map(ref -> String.format(
                        "name=%s version=%s source=%s origin=%s",
                        ref.moduleName(),
                        ref.moduleVersion(),
                        ref.source(),
                        ref.origin()
                    ))
                    .collect(Collectors.joining(" | "));
                return "moduleId=" + collision.moduleId() + " -> " + refs;
            })
            .collect(Collectors.joining("\n"));

        return "Module identity collision detected. Startup aborted.\n" + details;
    }
}
