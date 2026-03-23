package io.govaryn.kernel.module.validation;

import io.govaryn.kernel.module.ModuleMetadata;
import io.govaryn.kernel.module.ModuleType;
import io.govaryn.kernel.module.discovery.ModuleDiscoveryCandidate;
import io.govaryn.kernel.module.discovery.ModuleDiscoverySource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DefaultModuleValidator Tests")
class DefaultModuleValidatorTest {

    private final DefaultModuleValidator validator = new DefaultModuleValidator();

    @Test
    @DisplayName("Should produce valid report for compatible module")
    void shouldValidateCompatibleModule() {
        ModuleDiscoveryCandidate candidate = new ModuleDiscoveryCandidate(
            ModuleMetadata.minimal(
                "audit-log",
                "Audit Log",
                "1.2.0",
                "^1.0.0",
                ModuleType.OBSERVABILITY,
                "io.govaryn.modules.audit.AuditModule"
            ),
            ModuleDiscoverySource.CLASSPATH,
            "io.govaryn.modules.audit.AuditModule",
            true
        );

        List<ModuleValidationReport> reports = validator.validate(List.of(candidate), "1.5.0");
        ModuleValidationReport report = reports.getFirst();

        assertTrue(report.valid());
        assertTrue(report.issues().isEmpty());
    }

    @Test
    @DisplayName("Should report duplicate moduleId")
    void shouldReportDuplicateModuleId() {
        ModuleDiscoveryCandidate first = candidate("shared-id", "1.0.0", "^1.0.0");
        ModuleDiscoveryCandidate second = candidate("shared-id", "1.1.0", "^1.0.0");

        List<ModuleValidationReport> reports = validator.validate(List.of(first, second), "1.5.0");

        assertFalse(reports.get(0).valid());
        assertFalse(reports.get(1).valid());
        assertTrue(reports.get(0).issues().stream().anyMatch(i -> i.code() == ModuleValidationCode.DUPLICATE_MODULE_ID));
        assertTrue(reports.get(1).issues().stream().anyMatch(i -> i.code() == ModuleValidationCode.DUPLICATE_MODULE_ID));
    }

    @Test
    @DisplayName("Should report kernel API incompatibility")
    void shouldReportKernelApiIncompatibility() {
        ModuleDiscoveryCandidate candidate = candidate("orders", "1.0.0", "^2.0.0");

        List<ModuleValidationReport> reports = validator.validate(List.of(candidate), "1.3.0");
        ModuleValidationReport report = reports.getFirst();

        assertFalse(report.valid());
        assertTrue(report.issues().stream().anyMatch(i -> i.code() == ModuleValidationCode.KERNEL_API_INCOMPATIBLE));
    }

    @Test
    @DisplayName("Should reject incompatible caret range for major zero")
    void shouldRejectIncompatibleCaretRangeForMajorZero() {
        ModuleDiscoveryCandidate candidate = candidate("orders", "1.0.0", "^0.2.3");

        List<ModuleValidationReport> reports = validator.validate(List.of(candidate), "0.3.0");
        ModuleValidationReport report = reports.getFirst();

        assertFalse(report.valid());
        assertTrue(report.issues().stream().anyMatch(i -> i.code() == ModuleValidationCode.KERNEL_API_INCOMPATIBLE));
    }

    @Test
    @DisplayName("Should accept compatible caret range for major zero")
    void shouldAcceptCompatibleCaretRangeForMajorZero() {
        ModuleDiscoveryCandidate candidate = candidate("orders", "1.0.0", "^0.2.3");

        List<ModuleValidationReport> reports = validator.validate(List.of(candidate), "0.2.9");
        ModuleValidationReport report = reports.getFirst();

        assertTrue(report.valid());
    }

    @Test
    @DisplayName("Should report unsupported contract version")
    void shouldReportUnsupportedContractVersion() {
        ModuleMetadata metadata = new ModuleMetadata(
            "2.0.0",
            "billing",
            "Billing",
            "1.0.0",
            "^1.0.0",
            ModuleType.FEATURE,
            "io.govaryn.modules.billing.BillingModule",
            null,
            null,
            null,
            null,
            List.of(),
            List.of(),
            null,
            List.of()
        );

        ModuleDiscoveryCandidate candidate = new ModuleDiscoveryCandidate(
            metadata,
            ModuleDiscoverySource.MANIFEST_SCAN,
            "/tmp/billing/module.json",
            false
        );

        List<ModuleValidationReport> reports = validator.validate(List.of(candidate), "1.3.0");
        ModuleValidationReport report = reports.getFirst();

        assertFalse(report.valid());
        assertTrue(report.issues().stream().anyMatch(i -> i.code() == ModuleValidationCode.CONTRACT_VERSION_UNSUPPORTED));
    }

    private static ModuleDiscoveryCandidate candidate(String moduleId, String moduleVersion, String requiredKernelApiVersion) {
        return new ModuleDiscoveryCandidate(
            ModuleMetadata.minimal(
                moduleId,
                moduleId + "-module",
                moduleVersion,
                requiredKernelApiVersion,
                ModuleType.FEATURE,
                "io.govaryn.modules." + moduleId + ".Module"
            ),
            ModuleDiscoverySource.CLASSPATH,
            "io.govaryn.modules." + moduleId + ".Module",
            true
        );
    }
}
