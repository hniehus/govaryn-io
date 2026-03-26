package io.govaryn.kernel.module.discovery;

import io.govaryn.kernel.module.ModuleLifecycleState;
import io.govaryn.kernel.module.ModuleRegistry;
import io.govaryn.kernel.module.ModuleRegistryEntry;
import io.govaryn.kernel.module.validation.ModuleValidationReport;
import io.govaryn.kernel.module.validation.ModuleValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("module-reference-failing-init")
@TestPropertySource(properties = {
    "govaryn.kernel.id=test-kernel",
    "govaryn.kernel.environment=dev",
    "govaryn.kernel.module.mode=classpath",
    "spring.application.version=1.2.0"
})
@ExtendWith(OutputCaptureExtension.class)
@DisplayName("Reference Module Failure Path Integration Tests")
class ReferenceModuleFailurePathIntegrationTest {

    @Autowired
    private ModuleDiscoveryService moduleDiscoveryService;

    @Autowired
    private ModuleValidator moduleValidator;

    @Autowired
    private ModuleRegistry moduleRegistry;

    @Test
    @DisplayName("Reference module initialization failure path is handled and logged")
    void referenceModuleControlledFailurePath(CapturedOutput output) {
        // Discovery
        List<ModuleDiscoveryCandidate> candidates = moduleDiscoveryService.discover();
        Set<String> discoveredModuleIds = candidates.stream()
            .map(candidate -> candidate.metadata().moduleId())
            .collect(Collectors.toSet());
        assertTrue(discoveredModuleIds.contains("reference-feature"));
        assertTrue(discoveredModuleIds.contains("reference-platform-support"));

        // Validation
        List<ModuleValidationReport> reports = moduleValidator.validate(candidates, "1.2.0");
        assertTrue(reports.stream().allMatch(ModuleValidationReport::valid));

        // Registration + Initialization + Failure Handling
        ModuleRegistryEntry failedReference = moduleRegistry.findByModuleId("reference-feature").orElseThrow();
        ModuleRegistryEntry initializedSupport = moduleRegistry.findByModuleId("reference-platform-support").orElseThrow();

        assertEquals(ModuleLifecycleState.FAILED, failedReference.status().lifecycleState());
        assertEquals(ModuleLifecycleState.INITIALIZED, initializedSupport.status().lifecycleState());
        assertNotNull(failedReference.status().lastError());
        assertEquals("INITIALIZATION_FAILED", failedReference.status().lastError().errorCode());
        assertTrue(failedReference.status().lastError().message().contains("Reference feature module forced initialization failure"));

        assertTrue(output.getOut().contains("event=module_initialization_failed moduleId=reference-feature"));
        assertTrue(output.getOut().contains("policy=REJECT_MODULE_CONTINUE"));
        assertTrue(output.getOut().contains("event=module_startup_summary"));
        assertTrue(output.getOut().contains("failed=1"));
    }
}
