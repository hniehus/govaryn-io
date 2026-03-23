package io.govaryn.kernel.module;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("ModuleMetadata Contract Tests")
class ModuleMetadataContractTest {

    @Test
    @DisplayName("Should reject invalid moduleId format")
    void shouldRejectInvalidModuleIdFormat() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> ModuleMetadata.minimal(
                "INVALID_ID",
                "Invalid Module",
                "1.0.0",
                "^1.0.0",
                ModuleType.FEATURE,
                "io.govaryn.modules.InvalidModule"
            )
        );
        assertEquals("moduleId must match ^[a-z][a-z0-9-]{2,63}$", ex.getMessage());
    }

    @Test
    @DisplayName("Should reject non-https homepage")
    void shouldRejectNonHttpsHomepage() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> new ModuleMetadata(
                "1.0.0",
                "audit-log",
                "Audit Log",
                "1.0.0",
                "^1.0.0",
                ModuleType.OBSERVABILITY,
                "io.govaryn.modules.audit.AuditModule",
                null,
                null,
                null,
                "http://example.com",
                ModuleCapabilities.empty(),
                ModuleFailurePolicy.defaults(),
                null,
                List.of()
            )
        );
        assertEquals("homepage must start with https://", ex.getMessage());
    }
}
