package io.govaryn.kernel.module;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("KernelApiVersionCompatibility")
class KernelApiVersionCompatibilityTest {

    @Test
    @DisplayName("Should treat release as higher precedence than matching pre-release")
    void shouldTreatReleaseAsHigherPrecedenceThanMatchingPreRelease() {
        assertTrue(KernelApiVersionCompatibility.isCompatible(">=1.2.3-rc.1", "1.2.3"));
        assertFalse(KernelApiVersionCompatibility.isCompatible("1.2.3", "1.2.3-rc.1"));
    }

    @Test
    @DisplayName("Should enforce caret upper bound")
    void shouldEnforceCaretUpperBound() {
        assertTrue(KernelApiVersionCompatibility.isCompatible("^1.2.3", "1.9.0"));
        assertFalse(KernelApiVersionCompatibility.isCompatible("^1.2.3", "2.0.0"));
    }

    @Test
    @DisplayName("Should reject invalid running version")
    void shouldRejectInvalidRunningVersion() {
        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> KernelApiVersionCompatibility.validateRunningKernelApiVersion("1.x")
        );

        assertTrue(ex.getMessage().contains("runningKernelApiVersion"));
    }
}
