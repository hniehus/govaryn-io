package io.govaryn.kernel.module.validation;

import io.govaryn.kernel.module.ModuleMetadata;
import io.govaryn.kernel.module.ModuleType;
import io.govaryn.kernel.module.ModuleCapabilities;
import io.govaryn.kernel.module.ModuleFailurePolicy;
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
        ModuleValidationIssue issue = report.issues().stream()
            .filter(i -> i.code() == ModuleValidationCode.KERNEL_API_INCOMPATIBLE)
            .findFirst()
            .orElseThrow();
        assertTrue(issue.message().contains("moduleId 'orders'"));
        assertTrue(issue.message().contains("requiredKernelApiVersion '^2.0.0'"));
        assertTrue(issue.message().contains("running kernel API version '1.3.0'"));
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
            io.govaryn.kernel.module.ModuleCapabilities.empty(),
            io.govaryn.kernel.module.ModuleFailurePolicy.defaults(),
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

    @Test
    @DisplayName("Should report missing providedCapabilities as structured validation issue")
    void shouldReportMissingProvidedCapabilitiesAsStructuredIssue() {
        ModuleMetadata metadata = new ModuleMetadata(
            "1.0.0",
            "capability-missing",
            "Capability Missing",
            "1.0.0",
            "^1.0.0",
            ModuleType.FEATURE,
            "io.govaryn.modules.capability.MissingModule",
            null,
            null,
            null,
            null,
            ModuleCapabilities.empty(),
            ModuleFailurePolicy.defaults(),
            null,
            List.of()
        );

        ModuleDiscoveryCandidate candidate = new ModuleDiscoveryCandidate(
            metadata,
            ModuleDiscoverySource.MANIFEST_SCAN,
            "/plugins/capability-missing/module.json",
            false
        );

        ModuleValidationReport report = validator.validate(List.of(candidate), "1.2.0").getFirst();

        assertFalse(report.valid());
        ModuleValidationIssue issue = report.issues().stream()
            .filter(i -> i.fieldPath().equals("$.providedCapabilities"))
            .findFirst()
            .orElseThrow();
        assertEquals(ModuleValidationCode.REQUIRED_FIELD_MISSING, issue.code());
        assertEquals(ModuleValidationSeverity.ERROR, issue.severity());
        assertFalse(issue.message().isBlank());
    }

    @Test
    @DisplayName("Should report invalid requiredKernelApiVersion format")
    void shouldReportInvalidRequiredKernelApiVersionFormat() {
        ModuleDiscoveryCandidate candidate = new ModuleDiscoveryCandidate(
            ModuleMetadata.minimal(
                "invalid-range",
                "Invalid Range",
                "1.0.0",
                "1.x",
                ModuleType.FEATURE,
                "io.govaryn.modules.invalidrange.InvalidRangeModule"
            ),
            ModuleDiscoverySource.CLASSPATH,
            "io.govaryn.modules.invalidrange.InvalidRangeModule",
            true
        );

        ModuleValidationReport report = validator.validate(List.of(candidate), "1.2.0").getFirst();

        assertFalse(report.valid());
        assertTrue(hasIssue(report, ModuleValidationCode.INVALID_FIELD_VALUE, "$.requiredKernelApiVersion"));
    }

    @Test
    @DisplayName("Should fail fast when running kernel API version is invalid")
    void shouldFailFastForInvalidRunningKernelApiVersion() {
        ModuleDiscoveryCandidate candidate = candidate("orders", "1.0.0", "^1.0.0");

        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> validator.validate(List.of(candidate), "unknown")
        );

        assertTrue(ex.getMessage().contains("runningKernelApiVersion"));
        assertTrue(ex.getMessage().contains("unknown"));
    }

    @Test
    @DisplayName("Should reject exact release requirement when running kernel is pre-release")
    void shouldRejectExactReleaseRequirementWhenRunningKernelIsPreRelease() {
        ModuleDiscoveryCandidate candidate = candidate("orders", "1.0.0", "1.2.3");

        ModuleValidationReport report = validator.validate(List.of(candidate), "1.2.3-rc.1").getFirst();

        assertFalse(report.valid());
        assertTrue(hasIssue(report, ModuleValidationCode.KERNEL_API_INCOMPATIBLE, "$.requiredKernelApiVersion"));
    }

    @Test
    @DisplayName("Should accept release for minimum pre-release requirement")
    void shouldAcceptReleaseForMinimumPreReleaseRequirement() {
        ModuleDiscoveryCandidate candidate = candidate("orders", "1.0.0", ">=1.2.3-rc.1");

        ModuleValidationReport report = validator.validate(List.of(candidate), "1.2.3").getFirst();

        assertTrue(report.valid());
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

    private static boolean hasIssue(ModuleValidationReport report, ModuleValidationCode code, String fieldPath) {
        return report.issues().stream().anyMatch(issue -> issue.code() == code && fieldPath.equals(issue.fieldPath()));
    }
}
