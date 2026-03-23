package io.govaryn.kernel.module;

import io.govaryn.kernel.config.ModuleFailurePolicyAction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("ModuleFailurePolicy Tests")
class ModuleFailurePolicyTest {

    @Test
    @DisplayName("Should provide deterministic defaults")
    void shouldProvideDeterministicDefaults() {
        ModuleFailurePolicy defaults = ModuleFailurePolicy.defaults();

        assertEquals(ModuleFailurePolicyAction.REJECT_MODULE_CONTINUE, defaults.onInitializationFailure());
        assertEquals(ModuleFailurePolicyAction.MARK_MODULE_DEGRADED, defaults.onRuntimeFailure());
    }

    @Test
    @DisplayName("Should reject null actions")
    void shouldRejectNullActions() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new ModuleFailurePolicy(null, ModuleFailurePolicyAction.FAIL_FAST)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new ModuleFailurePolicy(ModuleFailurePolicyAction.FAIL_FAST, null)
        );
    }
}
