package io.govaryn.kernel.module.discovery;

import io.govaryn.kernel.module.ModuleLifecycleState;
import io.govaryn.kernel.module.ModuleRegistry;
import io.govaryn.kernel.module.ModuleRegistryEntry;
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
@ActiveProfiles("module-template")
@TestPropertySource(properties = {
    "govaryn.kernel.id=test-kernel",
    "govaryn.kernel.environment=dev",
    "govaryn.kernel.module.mode=classpath",
    "spring.application.version=1.2.0"
})
@DisplayName("Module Template Integration Tests")
class ModuleTemplateIntegrationTest {

    @Autowired
    private ModuleDiscoveryService moduleDiscoveryService;

    @Autowired
    private ModuleRegistry moduleRegistry;

    @Test
    @DisplayName("Template module is discoverable and fully registered at startup")
    void templateModuleIsDiscoverableAndRegistered() {
        List<ModuleDiscoveryCandidate> candidates = moduleDiscoveryService.discover();

        ModuleDiscoveryCandidate template = candidates.stream()
            .filter(candidate -> "template-module".equals(candidate.metadata().moduleId()))
            .findFirst()
            .orElseThrow();

        assertEquals(ModuleDiscoverySource.CLASSPATH, template.source());
        assertTrue(template.loadableInCurrentRuntime());
        assertFalse(template.metadata().capabilities().providedCapabilities().isEmpty());
        assertFalse(template.metadata().capabilities().requiredCapabilities().isEmpty());

        ModuleRegistryEntry registeredTemplate = moduleRegistry.findByModuleId("template-module").orElseThrow();
        assertEquals(ModuleLifecycleState.INITIALIZED, registeredTemplate.status().lifecycleState());
    }
}
