package io.govaryn.kernel.module;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ModuleCapabilities Tests")
class ModuleCapabilitiesTest {

    @Test
    @DisplayName("Should create valid capabilities declaration")
    void shouldCreateValidCapabilitiesDeclaration() {
        ModuleCapabilities capabilities = new ModuleCapabilities(
            List.of("audit.write", "audit.query"),
            List.of("event.publish"),
            List.of("metrics.counter")
        );

        assertEquals(List.of("audit.write", "audit.query"), capabilities.providedCapabilities());
        assertEquals(List.of("event.publish"), capabilities.requiredCapabilities());
        assertEquals(List.of("metrics.counter"), capabilities.optionalCapabilities());
    }

    @Test
    @DisplayName("Should reject overlapping required and optional capabilities")
    void shouldRejectOverlappingRequiredAndOptionalCapabilities() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> new ModuleCapabilities(
                List.of("audit.write"),
                List.of("event.publish"),
                List.of("event.publish")
            )
        );
        assertTrue(ex.getMessage().contains("must be disjoint"));
    }

    @Test
    @DisplayName("Should reject invalid capability identifier")
    void shouldRejectInvalidCapabilityIdentifier() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> new ModuleCapabilities(
                List.of("Audit.Write"),
                List.of(),
                List.of()
            )
        );
        assertTrue(ex.getMessage().contains("invalid capability id"));
    }
}
