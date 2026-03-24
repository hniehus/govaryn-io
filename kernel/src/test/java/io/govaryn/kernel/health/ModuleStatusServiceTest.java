package io.govaryn.kernel.health;

import io.govaryn.kernel.module.InMemoryModuleRegistry;
import io.govaryn.kernel.module.ModuleFailureDetails;
import io.govaryn.kernel.module.ModuleLifecycleState;
import io.govaryn.kernel.module.ModuleMetadata;
import io.govaryn.kernel.module.ModuleType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("Module Status Service Tests")
class ModuleStatusServiceTest {

    @Test
    @DisplayName("Should expose module status programmatically")
    void shouldExposeModuleStatusProgrammatically() {
        InMemoryModuleRegistry registry = new InMemoryModuleRegistry();
        ModuleMetadata metadata = ModuleMetadata.minimal(
            "demo-module",
            "DemoModule",
            "1.0.0",
            "^1.0.0",
            ModuleType.FEATURE,
            "io.govaryn.modules.DemoModule"
        );

        registry.registerValidated(metadata, false, metadata.entryPoint(), "1.2.0");
        ModuleStatusService service = new ModuleStatusService(registry);

        ModuleStatusResponse response = service.currentStatus();

        assertEquals(1, response.modules().size());
        ModuleStatusItemResponse module = response.modules().getFirst();
        assertEquals("demo-module", module.moduleId());
        assertEquals("1.0.0", module.moduleVersion());
        assertEquals("REGISTERED", module.moduleStatus());
        assertEquals("REJECT_MODULE_CONTINUE", module.failurePolicy());
        assertNull(module.errorCause());
    }

    @Test
    @DisplayName("Should expose failed module error cause in status response")
    void shouldExposeErrorCauseForFailedModule() {
        InMemoryModuleRegistry registry = new InMemoryModuleRegistry();
        ModuleMetadata metadata = ModuleMetadata.minimal(
            "broken-module",
            "BrokenModule",
            "1.0.0",
            "^1.0.0",
            ModuleType.FEATURE,
            "io.govaryn.modules.BrokenModule"
        );

        registry.registerValidated(metadata, false, metadata.entryPoint(), "1.2.0");
        registry.transitionState(metadata.moduleId(), ModuleLifecycleState.INITIALIZING, null);
        registry.transitionState(
            metadata.moduleId(),
            ModuleLifecycleState.FAILED,
            new ModuleFailureDetails("INITIALIZATION_FAILED", "boom")
        );

        ModuleStatusService service = new ModuleStatusService(registry);
        ModuleStatusItemResponse module = service.currentStatus().modules().getFirst();

        assertEquals("FAILED", module.moduleStatus());
        assertEquals("boom", module.errorCause());
    }
}
