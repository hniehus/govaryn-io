package io.govaryn.kernel.module.identity;

import io.govaryn.kernel.module.InMemoryModuleRegistry;
import io.govaryn.kernel.module.ModuleMetadata;
import io.govaryn.kernel.module.ModuleType;
import io.govaryn.kernel.module.discovery.ModuleDiscoveryCandidate;
import io.govaryn.kernel.module.discovery.ModuleDiscoverySource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ModuleIdentityCollisionDetector Tests")
class ModuleIdentityCollisionDetectorTest {

    @Test
    @DisplayName("Should detect same moduleId across discovered candidates")
    void shouldDetectCollisionAcrossCandidates() {
        ModuleIdentityCollisionDetector detector = new ModuleIdentityCollisionDetector();
        InMemoryModuleRegistry registry = new InMemoryModuleRegistry();

        ModuleDiscoveryCandidate first = candidate(
            "shared-id",
            "ClasspathModule",
            "io.govaryn.modules.ClasspathModule",
            ModuleDiscoverySource.CLASSPATH
        );
        ModuleDiscoveryCandidate second = candidate(
            "shared-id",
            "PluginModule",
            "/plugins/plugin/module.json",
            ModuleDiscoverySource.MANIFEST_SCAN
        );

        List<ModuleIdentityCollision> collisions = detector.detect(List.of(first, second), registry);

        assertEquals(1, collisions.size());
        assertEquals("shared-id", collisions.getFirst().moduleId());
        assertEquals(2, collisions.getFirst().references().size());

        String message = detector.formatCollisionMessage(collisions);
        assertTrue(message.contains("Module identity collision detected"));
        assertTrue(message.contains("shared-id"));
        assertTrue(message.contains("ClasspathModule"));
        assertTrue(message.contains("PluginModule"));
    }

    @Test
    @DisplayName("Should detect collision with already registered module")
    void shouldDetectCollisionWithRegistryEntry() {
        ModuleIdentityCollisionDetector detector = new ModuleIdentityCollisionDetector();
        InMemoryModuleRegistry registry = new InMemoryModuleRegistry();
        registry.registerValidated(
            ModuleMetadata.minimal(
                "audit-log",
                "AuditModule",
                "1.0.0",
                "^1.0.0",
                ModuleType.OBSERVABILITY,
                "io.govaryn.modules.AuditModule"
            ),
            true,
            "classpath:AuditModule",
            "1.2.0"
        );

        ModuleDiscoveryCandidate candidate = candidate(
            "audit-log",
            "AuditPluginModule",
            "/plugins/audit/module.json",
            ModuleDiscoverySource.MANIFEST_SCAN
        );

        List<ModuleIdentityCollision> collisions = detector.detect(List.of(candidate), registry);

        assertEquals(1, collisions.size());
        assertEquals(2, collisions.getFirst().references().size());
        String message = detector.formatCollisionMessage(collisions);
        assertTrue(message.contains("classpath:AuditModule"));
        assertTrue(message.contains("/plugins/audit/module.json"));
    }

    @Test
    @DisplayName("Should include registry entry only once for duplicated candidate moduleId")
    void shouldIncludeRegistryEntryOnlyOnceForDuplicatedCandidateModuleId() {
        ModuleIdentityCollisionDetector detector = new ModuleIdentityCollisionDetector();
        InMemoryModuleRegistry registry = new InMemoryModuleRegistry();
        registry.registerValidated(
            ModuleMetadata.minimal(
                "shared-id",
                "RegisteredModule",
                "1.0.0",
                "^1.0.0",
                ModuleType.FEATURE,
                "io.govaryn.modules.RegisteredModule"
            ),
            true,
            "classpath:RegisteredModule",
            "1.2.0"
        );

        ModuleDiscoveryCandidate first = candidate(
            "shared-id",
            "PluginModuleA",
            "/plugins/a/module.json",
            ModuleDiscoverySource.MANIFEST_SCAN
        );
        ModuleDiscoveryCandidate second = candidate(
            "shared-id",
            "PluginModuleB",
            "/plugins/b/module.json",
            ModuleDiscoverySource.MANIFEST_SCAN
        );

        List<ModuleIdentityCollision> collisions = detector.detect(List.of(first, second), registry);

        assertEquals(1, collisions.size());
        assertEquals(3, collisions.getFirst().references().size());
    }

    private static ModuleDiscoveryCandidate candidate(
        String moduleId,
        String moduleName,
        String origin,
        ModuleDiscoverySource source
    ) {
        return new ModuleDiscoveryCandidate(
            ModuleMetadata.minimal(
                moduleId,
                moduleName,
                "1.0.0",
                "^1.0.0",
                ModuleType.FEATURE,
                "io.govaryn.modules." + moduleName
            ),
            source,
            origin,
            source == ModuleDiscoverySource.CLASSPATH
        );
    }
}
