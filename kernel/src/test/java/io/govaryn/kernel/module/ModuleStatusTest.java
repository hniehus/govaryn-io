package io.govaryn.kernel.module;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ModuleStatus Tests")
class ModuleStatusTest {

    @Test
    @DisplayName("Should create discovered status")
    void shouldCreateDiscoveredStatus() {
        ModuleStatus discovered = ModuleStatus.discovered();

        assertEquals(ModuleLifecycleState.DISCOVERED, discovered.lifecycleState());
        assertFalse(discovered.degraded());
    }

    @Test
    @DisplayName("Should create degraded status with dedicated lifecycle state")
    void shouldCreateDegradedStatusWithDedicatedLifecycleState() {
        ModuleFailureDetails failure = new ModuleFailureDetails("RUNTIME_FAILURE", "backend unavailable");

        ModuleStatus degraded = ModuleStatus.degraded(failure);

        assertEquals(ModuleLifecycleState.DEGRADED, degraded.lifecycleState());
        assertTrue(degraded.degraded());
        assertEquals("RUNTIME_FAILURE", degraded.lastError().errorCode());
    }
}
