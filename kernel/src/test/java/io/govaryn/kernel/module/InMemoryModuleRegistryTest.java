package io.govaryn.kernel.module;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InMemoryModuleRegistry Tests")
class InMemoryModuleRegistryTest {

    @Test
    @DisplayName("Should register discovered module and transition through valid lifecycle")
    void shouldRegisterAndTransitionLifecycle() {
        InMemoryModuleRegistry registry = new InMemoryModuleRegistry();
        ModuleMetadata metadata = ModuleMetadata.minimal(
            "audit-log",
            "AuditLogModule",
            "1.0.0",
            "^1.0.0",
            ModuleType.OBSERVABILITY,
            "io.govaryn.modules.audit.AuditLogModule"
        );

        registry.addDiscovered(metadata, true);
        registry.transitionState("audit-log", ModuleLifecycleState.VALIDATED, null);
        registry.transitionState("audit-log", ModuleLifecycleState.REGISTERED, null);
        registry.transitionState("audit-log", ModuleLifecycleState.INITIALIZING, null);
        registry.transitionState("audit-log", ModuleLifecycleState.INITIALIZED, null);

        ModuleRegistryEntry entry = registry.findByModuleId("audit-log").orElseThrow();
        assertEquals(ModuleLifecycleState.INITIALIZED, entry.status().lifecycleState());
        assertFalse(entry.status().degraded());
    }

    @Test
    @DisplayName("Should register validated module directly with state REGISTERED")
    void shouldRegisterValidatedDirectly() {
        InMemoryModuleRegistry registry = new InMemoryModuleRegistry();
        ModuleMetadata metadata = ModuleMetadata.minimal(
            "billing",
            "BillingModule",
            "1.0.0",
            "^1.0.0",
            ModuleType.FEATURE,
            "io.govaryn.modules.billing.BillingModule"
        );

        registry.registerValidated(metadata, true, "classpath:BillingModule", "1.2.0");

        ModuleRegistryEntry entry = registry.findByModuleId("billing").orElseThrow();
        assertEquals(ModuleLifecycleState.REGISTERED, entry.status().lifecycleState());
        assertEquals("classpath:BillingModule", entry.origin());
        assertTrue(entry.mandatory());
    }

    @Test
    @DisplayName("Should reject duplicate moduleId")
    void shouldRejectDuplicateModuleId() {
        InMemoryModuleRegistry registry = new InMemoryModuleRegistry();
        ModuleMetadata metadata = ModuleMetadata.minimal(
            "audit-log",
            "AuditLogModule",
            "1.0.0",
            "^1.0.0",
            ModuleType.OBSERVABILITY,
            "io.govaryn.modules.audit.AuditLogModule"
        );

        registry.addDiscovered(metadata, false, "classpath:AuditLogModule");

        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> registry.addDiscovered(metadata, false, "plugins/audit/module.json")
        );
        assertTrue(ex.getMessage().contains("Duplicate moduleId"));
        assertTrue(ex.getMessage().contains("existing"));
        assertTrue(ex.getMessage().contains("incoming"));
    }

    @Test
    @DisplayName("Should reject registration of incompatible module")
    void shouldRejectIncompatibleModuleRegistration() {
        InMemoryModuleRegistry registry = new InMemoryModuleRegistry();
        ModuleMetadata metadata = ModuleMetadata.minimal(
            "incompatible-module",
            "IncompatibleModule",
            "1.0.0",
            "^2.0.0",
            ModuleType.FEATURE,
            "io.govaryn.modules.incompatible.IncompatibleModule"
        );

        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> registry.registerValidated(metadata, false, "classpath:IncompatibleModule", "1.2.0")
        );
        assertTrue(ex.getMessage().contains("incompatible-module"));
        assertTrue(ex.getMessage().contains("^2.0.0"));
        assertTrue(ex.getMessage().contains("1.2.0"));
        assertTrue(registry.findByModuleId("incompatible-module").isEmpty());
    }

    @Test
    @DisplayName("Should reject invalid lifecycle transition")
    void shouldRejectInvalidTransition() {
        InMemoryModuleRegistry registry = new InMemoryModuleRegistry();
        ModuleMetadata metadata = ModuleMetadata.minimal(
            "event-bus",
            "EventBusModule",
            "1.0.0",
            "^1.0.0",
            ModuleType.CORE_EXTENSION,
            "io.govaryn.modules.event.EventBusModule"
        );
        registry.addDiscovered(metadata, true);

        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> registry.transitionState("event-bus", ModuleLifecycleState.INITIALIZED, null)
        );
        assertTrue(ex.getMessage().contains("Invalid state transition"));
    }

    @Test
    @DisplayName("Should mark initialized module as degraded")
    void shouldMarkInitializedModuleAsDegraded() {
        InMemoryModuleRegistry registry = new InMemoryModuleRegistry();
        ModuleMetadata metadata = ModuleMetadata.minimal(
            "telemetry",
            "TelemetryModule",
            "1.0.0",
            "^1.0.0",
            ModuleType.OBSERVABILITY,
            "io.govaryn.modules.telemetry.TelemetryModule"
        );

        registry.addDiscovered(metadata, false);
        registry.transitionState("telemetry", ModuleLifecycleState.VALIDATED, null);
        registry.transitionState("telemetry", ModuleLifecycleState.REGISTERED, null);
        registry.transitionState("telemetry", ModuleLifecycleState.INITIALIZING, null);
        registry.transitionState("telemetry", ModuleLifecycleState.INITIALIZED, null);

        ModuleFailureDetails failure = new ModuleFailureDetails("RUNTIME_FAILURE", "Temporary backend outage");
        registry.markDegraded("telemetry", failure);

        ModuleRegistryEntry entry = registry.findByModuleId("telemetry").orElseThrow();
        assertTrue(entry.status().degraded());
        assertEquals("RUNTIME_FAILURE", entry.status().lastError().errorCode());
        assertEquals(ModuleLifecycleState.DEGRADED, entry.status().lifecycleState());
    }
}
