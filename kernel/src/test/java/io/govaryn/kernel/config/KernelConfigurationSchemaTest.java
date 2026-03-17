package io.govaryn.kernel.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for KernelConfigurationSchema.
 * Includes good cases (valid keys and metadata) and bad cases (invalid keys).
 */
class KernelConfigurationSchemaTest {

    @Test
    void goodCase_allowedKeysAreRecognized() {
        // Good case: Valid keys are allowed and have correct metadata
        assertThat(KernelConfigurationSchema.isKeyAllowed("govaryn.kernel.id")).isTrue();
        assertThat(KernelConfigurationSchema.isKeyAllowed("govaryn.kernel.environment")).isTrue();
        assertThat(KernelConfigurationSchema.isKeyAllowed("govaryn.kernel.module.mode")).isTrue();

        // Check metadata for one key
        var metadata = KernelConfigurationSchema.getMetadata("govaryn.kernel.id");
        assertThat(metadata).isNotNull();
        assertThat(metadata.name()).isEqualTo("govaryn.kernel.id");
        assertThat(metadata.type()).isEqualTo(KernelConfigurationSchema.ConfigType.STRING);
        assertThat(metadata.required()).isTrue();
        assertThat(metadata.strictness()).isEqualTo(KernelConfigurationSchema.Strictness.NON_STRICT);
    }

    @Test
    void badCase_invalidKeysAreRejected() {
        // Bad case: Invalid keys are not allowed and return null metadata
        assertThat(KernelConfigurationSchema.isKeyAllowed("govaryn.kernel.unknown")).isFalse();
        assertThat(KernelConfigurationSchema.isKeyAllowed("spring.application.name")).isFalse();
        assertThat(KernelConfigurationSchema.isKeyAllowed("")).isFalse();

        // Check that metadata is null for invalid key
        var metadata = KernelConfigurationSchema.getMetadata("govaryn.kernel.unknown");
        assertThat(metadata).isNull();
    }

    @Test
    void getAllowedKeysReturnsAllDefinedKeys() {
        List<String> allowedKeys = KernelConfigurationSchema.getAllowedKeys();
        assertThat(allowedKeys).containsExactlyInAnyOrder(
            "govaryn.kernel.id",
            "govaryn.kernel.environment",
            "govaryn.kernel.module.mode"
        );
    }
}
