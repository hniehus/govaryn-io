package io.govaryn.kernel;

import io.govaryn.kernel.config.GovarynKernelProperties;
import io.govaryn.kernel.config.KernelEnvironment;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GovarynKernelPropertiesValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsMissingRequiredProperties() {
        GovarynKernelProperties properties = new GovarynKernelProperties();

        var violations = validator.validate(properties);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("id", "environment");
    }

    @Test
    void acceptsMinimalValidProperties() {
        GovarynKernelProperties properties = new GovarynKernelProperties();
        properties.setId("local-kernel");
        properties.setEnvironment(KernelEnvironment.DEV);

        var violations = validator.validate(properties);

        assertThat(violations).isEmpty();
    }
}
