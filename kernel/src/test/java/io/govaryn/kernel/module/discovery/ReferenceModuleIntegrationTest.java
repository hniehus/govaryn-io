package io.govaryn.kernel.module.discovery;

import io.govaryn.kernel.module.ModuleLifecycleState;
import io.govaryn.kernel.module.ModuleRegistry;
import io.govaryn.kernel.module.ModuleRegistryEntry;
import io.govaryn.kernel.module.validation.ModuleValidationReport;
import io.govaryn.kernel.module.validation.ModuleValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("module-reference")
@TestPropertySource(properties = {
    "govaryn.kernel.id=test-kernel",
    "govaryn.kernel.environment=dev",
    "govaryn.kernel.module.mode=classpath",
    "spring.application.version=1.2.0"
})
@ExtendWith(OutputCaptureExtension.class)
@DisplayName("Reference Module Integration Tests")
class ReferenceModuleIntegrationTest {

    @Autowired
    private ModuleDiscoveryService moduleDiscoveryService;

    @Autowired
    private ModuleValidator moduleValidator;

    @Autowired
    private ModuleRegistry moduleRegistry;

    @Test
    @DisplayName("Reference modules complete full lifecycle in happy path")
    void referenceModuleFullStartupPath(CapturedOutput output) {
        // Discovery
        List<ModuleDiscoveryCandidate> candidates = moduleDiscoveryService.discover();
        Set<String> discoveredModuleIds = candidates.stream()
            .map(candidate -> candidate.metadata().moduleId())
            .collect(Collectors.toSet());
        assertTrue(discoveredModuleIds.contains("reference-feature"));
        assertTrue(discoveredModuleIds.contains("reference-platform-support"));

        ModuleDiscoveryCandidate reference = candidates.stream()
            .filter(candidate -> "reference-feature".equals(candidate.metadata().moduleId()))
            .findFirst()
            .orElseThrow();

        assertEquals(ModuleDiscoverySource.CLASSPATH, reference.source());
        assertTrue(reference.loadableInCurrentRuntime());
        assertFalse(reference.metadata().capabilities().providedCapabilities().isEmpty());
        assertFalse(reference.metadata().capabilities().requiredCapabilities().isEmpty());

        // Validation
        List<ModuleValidationReport> reports = moduleValidator.validate(candidates, "1.2.0");
        assertEquals(candidates.size(), reports.size());
        assertTrue(reports.stream().allMatch(ModuleValidationReport::valid));

        // Registration + Initialization (performed by orchestrator at startup)
        ModuleRegistryEntry registeredReference = moduleRegistry.findByModuleId("reference-feature").orElseThrow();
        ModuleRegistryEntry registeredSupport = moduleRegistry.findByModuleId("reference-platform-support").orElseThrow();

        assertNotNull(registeredReference.origin());
        assertNotNull(registeredSupport.origin());
        assertEquals(ModuleLifecycleState.INITIALIZED, registeredReference.status().lifecycleState());
        assertEquals(ModuleLifecycleState.INITIALIZED, registeredSupport.status().lifecycleState());

        assertTrue(output.getOut().contains("event=module_startup_summary"));
        assertTrue(output.getOut().contains("failed=0"));
    }
}
