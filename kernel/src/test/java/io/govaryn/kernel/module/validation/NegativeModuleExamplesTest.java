package io.govaryn.kernel.module.validation;

import io.govaryn.kernel.api.KernelContext;
import io.govaryn.kernel.module.ModuleMetadata;
import io.govaryn.kernel.module.discovery.ModuleDiscoveryCandidate;
import io.govaryn.kernel.module.discovery.ModuleDiscoverySource;
import io.govaryn.modules.examples.DuplicateIdModuleA;
import io.govaryn.modules.examples.DuplicateIdModuleB;
import io.govaryn.modules.examples.FailingInitializationModule;
import io.govaryn.modules.examples.IncompatibleApiVersionModule;
import io.govaryn.modules.examples.MissingRequiredFieldModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Negative Module Examples Tests")
class NegativeModuleExamplesTest {

    private final DefaultModuleValidator validator = new DefaultModuleValidator();

    @Test
    @DisplayName("Missing required field example fails metadata construction")
    void missingRequiredFieldExampleFailsMetadataConstruction() {
        MissingRequiredFieldModule module = new MissingRequiredFieldModule();
        assertThrows(IllegalArgumentException.class, module::metadata);
    }

    @Test
    @DisplayName("Incompatible API version example is rejected by validator")
    void incompatibleApiVersionExampleIsRejected() {
        IncompatibleApiVersionModule module = new IncompatibleApiVersionModule();
        ModuleDiscoveryCandidate candidate = candidate(module.metadata(), module.getClass().getName());

        ModuleValidationReport report = validator.validate(List.of(candidate), "1.2.0").getFirst();

        assertFalse(report.valid());
        assertTrue(report.issues().stream().anyMatch(i -> i.code() == ModuleValidationCode.KERNEL_API_INCOMPATIBLE));
    }

    @Test
    @DisplayName("Duplicate module ID examples are flagged")
    void duplicateModuleIdExamplesAreFlagged() {
        DuplicateIdModuleA moduleA = new DuplicateIdModuleA();
        DuplicateIdModuleB moduleB = new DuplicateIdModuleB();

        List<ModuleValidationReport> reports = validator.validate(
            List.of(
                candidate(moduleA.metadata(), moduleA.getClass().getName()),
                candidate(moduleB.metadata(), moduleB.getClass().getName())
            ),
            "1.2.0"
        );

        assertEquals(2, reports.size());
        assertTrue(reports.stream().allMatch(report ->
            report.issues().stream().anyMatch(issue -> issue.code() == ModuleValidationCode.DUPLICATE_MODULE_ID)
        ));
    }

    @Test
    @DisplayName("Initialization failure example throws during initialize")
    void initializationFailureExampleThrows() {
        FailingInitializationModule module = new FailingInitializationModule();
        KernelContext context = new KernelContext("test-kernel", io.govaryn.kernel.config.KernelEnvironment.DEV, "1.2.0");

        assertThrows(IllegalStateException.class, () -> module.initialize(context));
    }

    private static ModuleDiscoveryCandidate candidate(ModuleMetadata metadata, String origin) {
        return new ModuleDiscoveryCandidate(metadata, ModuleDiscoverySource.CLASSPATH, origin, true);
    }
}
