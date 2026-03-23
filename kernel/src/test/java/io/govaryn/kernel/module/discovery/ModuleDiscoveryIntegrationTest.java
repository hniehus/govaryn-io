package io.govaryn.kernel.module.discovery;

import io.govaryn.kernel.module.ModuleRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("module-example-minimal")
@TestPropertySource(properties = {
    "govaryn.kernel.id=test-kernel",
    "govaryn.kernel.environment=dev",
    "govaryn.kernel.module.mode=classpath",
    "spring.application.version=1.2.0"
})
@DisplayName("Module Discovery Integration Tests")
class ModuleDiscoveryIntegrationTest {

    @Autowired
    private ModuleDiscoveryService moduleDiscoveryService;

    @Autowired
    private ModuleRegistry moduleRegistry;

    @Test
    @DisplayName("Should discover module candidates with structured metadata on startup")
    void shouldDiscoverModuleCandidatesWithStructuredMetadataOnStartup() {
        int registryEntriesBefore = moduleRegistry.findAll().size();

        List<ModuleDiscoveryCandidate> candidates = moduleDiscoveryService.discover();

        int registryEntriesAfter = moduleRegistry.findAll().size();

        assertFalse(candidates.isEmpty());
        ModuleDiscoveryCandidate minimal = candidates.stream()
            .filter(candidate -> "reference-minimal".equals(candidate.metadata().moduleId()))
            .findFirst()
            .orElseThrow();

        assertEquals(ModuleDiscoverySource.CLASSPATH, minimal.source());
        assertTrue(minimal.loadableInCurrentRuntime());
        assertEquals("Reference Minimal Module", minimal.metadata().moduleName());
        assertEquals("1.0.0", minimal.metadata().moduleVersion());
        assertEquals("^1.0.0", minimal.metadata().requiredKernelApiVersion());

        // Discovery itself must be read-only for registry state.
        assertEquals(registryEntriesBefore, registryEntriesAfter);
    }
}
