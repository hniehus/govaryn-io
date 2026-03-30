package io.govaryn.kernel.module.validation;

import io.govaryn.kernel.module.ModuleMetadata;
import io.govaryn.kernel.module.KernelApiVersionCompatibility;
import io.govaryn.kernel.module.discovery.ModuleDiscoveryCandidate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class DefaultModuleValidator implements ModuleValidator {

    private static final Pattern MODULE_ID_PATTERN = Pattern.compile("^[a-z][a-z0-9-]{2,63}$");
    private static final Pattern CAPABILITY_PATTERN = Pattern.compile("^[a-z][a-z0-9.:-]{2,127}$");
    private static final Pattern SEMVER_PATTERN = Pattern.compile(
        "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*)?(?:\\+[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*)?$"
    );
    private static final String SUPPORTED_CONTRACT_VERSION = "1.0.0";

    @Override
    public List<ModuleValidationReport> validate(List<ModuleDiscoveryCandidate> candidates, String runningKernelApiVersion) {
        validateRunningKernelApiVersion(runningKernelApiVersion);

        Map<String, Integer> moduleIdCounts = new HashMap<>();
        for (ModuleDiscoveryCandidate candidate : candidates) {
            moduleIdCounts.merge(candidate.metadata().moduleId(), 1, Integer::sum);
        }

        List<ModuleValidationReport> reports = new ArrayList<>(candidates.size());
        for (ModuleDiscoveryCandidate candidate : candidates) {
            ModuleMetadata metadata = candidate.metadata();
            List<ModuleValidationIssue> issues = new ArrayList<>();

            validateRequiredFields(metadata, issues);
            validateFormats(metadata, issues);
            validateContractVersion(metadata, issues);
            validateContractSemantics(metadata, issues);
            validateDuplicateModuleId(metadata, moduleIdCounts, issues);
            validateKernelApiCompatibility(metadata, runningKernelApiVersion, issues);

            boolean valid = issues.stream().noneMatch(issue -> issue.severity() == ModuleValidationSeverity.ERROR);
            reports.add(new ModuleValidationReport(
                metadata.moduleId(),
                metadata.moduleName(),
                candidate.source(),
                candidate.origin(),
                valid,
                issues
            ));
        }

        return List.copyOf(reports);
    }

    private static void validateRequiredFields(ModuleMetadata metadata, List<ModuleValidationIssue> issues) {
        required(metadata.moduleContractVersion(), "$.moduleContractVersion", issues);
        required(metadata.moduleId(), "$.moduleId", issues);
        required(metadata.moduleName(), "$.moduleName", issues);
        required(metadata.moduleVersion(), "$.moduleVersion", issues);
        required(metadata.requiredKernelApiVersion(), "$.requiredKernelApiVersion", issues);
        required(metadata.entryPoint(), "$.entryPoint", issues);
        if (metadata.moduleType() == null) {
            issues.add(issue(
                ModuleValidationCode.REQUIRED_FIELD_MISSING,
                "$.moduleType",
                "Required field is missing: moduleType"
            ));
        }
        if (metadata.capabilities() == null) {
            issues.add(issue(
                ModuleValidationCode.REQUIRED_FIELD_MISSING,
                "$.capabilities",
                "Required field is missing: capabilities"
            ));
        } else {
            if (metadata.capabilities().providedCapabilities() == null) {
                issues.add(issue(
                    ModuleValidationCode.REQUIRED_FIELD_MISSING,
                    "$.providedCapabilities",
                    "Required field is missing: providedCapabilities"
                ));
            }
            if (metadata.capabilities().requiredCapabilities() == null) {
                issues.add(issue(
                    ModuleValidationCode.REQUIRED_FIELD_MISSING,
                    "$.requiredCapabilities",
                    "Required field is missing: requiredCapabilities"
                ));
            }
        }
        if (metadata.failurePolicy() == null) {
            issues.add(issue(
                ModuleValidationCode.REQUIRED_FIELD_MISSING,
                "$.failurePolicy",
                "Required field is missing: failurePolicy"
            ));
        }
    }

    private static void validateFormats(ModuleMetadata metadata, List<ModuleValidationIssue> issues) {
        if (!isBlank(metadata.moduleId()) && !MODULE_ID_PATTERN.matcher(metadata.moduleId()).matches()) {
            issues.add(issue(
                ModuleValidationCode.INVALID_FIELD_VALUE,
                "$.moduleId",
                "moduleId must match ^[a-z][a-z0-9-]{2,63}$"
            ));
        }

        if (!isBlank(metadata.moduleVersion()) && !SEMVER_PATTERN.matcher(metadata.moduleVersion()).matches()) {
            issues.add(issue(
                ModuleValidationCode.INVALID_FIELD_VALUE,
                "$.moduleVersion",
                "moduleVersion must be a semantic version"
            ));
        }

        if (!isBlank(metadata.homepage()) && !metadata.homepage().startsWith("https://")) {
            issues.add(issue(
                ModuleValidationCode.INVALID_FIELD_VALUE,
                "$.homepage",
                "homepage must start with https://"
            ));
        }

        if (!isBlank(metadata.requiredKernelApiVersion())
            && !KernelApiVersionCompatibility.isSupportedVersionRange(metadata.requiredKernelApiVersion())) {
            issues.add(issue(
                ModuleValidationCode.INVALID_FIELD_VALUE,
                "$.requiredKernelApiVersion",
                "requiredKernelApiVersion must be a supported range or exact version"
            ));
        }
    }

    private static void validateContractVersion(ModuleMetadata metadata, List<ModuleValidationIssue> issues) {
        if (!isBlank(metadata.moduleContractVersion()) && !SUPPORTED_CONTRACT_VERSION.equals(metadata.moduleContractVersion())) {
            issues.add(issue(
                ModuleValidationCode.CONTRACT_VERSION_UNSUPPORTED,
                "$.moduleContractVersion",
                "Unsupported moduleContractVersion: " + metadata.moduleContractVersion()
            ));
        }
    }

    private static void validateContractSemantics(ModuleMetadata metadata, List<ModuleValidationIssue> issues) {
        if (metadata.capabilities() == null) {
            return;
        }
        if (metadata.capabilities().providedCapabilities() == null || metadata.capabilities().providedCapabilities().isEmpty()) {
            issues.add(issue(
                ModuleValidationCode.REQUIRED_FIELD_MISSING,
                "$.providedCapabilities",
                "providedCapabilities must contain at least one capability"
            ));
        }
        validateCapabilities(metadata, issues);
    }

    private static void validateCapabilities(ModuleMetadata metadata, List<ModuleValidationIssue> issues) {
        List<String> provided = metadata.capabilities().providedCapabilities();
        List<String> required = metadata.capabilities().requiredCapabilities();
        List<String> optional = metadata.capabilities().optionalCapabilities();

        validateCapabilityList(provided, "$.providedCapabilities", issues);
        validateCapabilityList(required, "$.requiredCapabilities", issues);
        validateCapabilityList(optional, "$.optionalCapabilities", issues);

        ensureDisjoint(provided, required, "$.providedCapabilities", "$.requiredCapabilities", issues);
        ensureDisjoint(required, optional, "$.requiredCapabilities", "$.optionalCapabilities", issues);
    }

    private static void validateCapabilityList(List<String> capabilities, String fieldPath, List<ModuleValidationIssue> issues) {
        if (capabilities == null) {
            return;
        }

        Set<String> seen = new LinkedHashSet<>();
        for (int i = 0; i < capabilities.size(); i++) {
            String capability = capabilities.get(i);
            String itemPath = fieldPath + "[" + i + "]";

            if (isBlank(capability)) {
                issues.add(capabilityIssue(itemPath, "Capability must not be blank"));
                continue;
            }

            String trimmed = capability.trim();
            if (!CAPABILITY_PATTERN.matcher(trimmed).matches()) {
                issues.add(capabilityIssue(itemPath, "Capability id must match ^[a-z][a-z0-9.:-]{2,127}$"));
            }

            if (!seen.add(trimmed)) {
                issues.add(capabilityIssue(itemPath, "Duplicate capability id detected: " + trimmed));
            }
        }
    }

    private static void ensureDisjoint(
        List<String> left,
        List<String> right,
        String leftPath,
        String rightPath,
        List<ModuleValidationIssue> issues
    ) {
        if (left == null || right == null) {
            return;
        }
        Set<String> leftValues = normalizeCapabilities(left);
        Set<String> rightValues = normalizeCapabilities(right);
        leftValues.retainAll(rightValues);
        if (!leftValues.isEmpty()) {
            issues.add(capabilityIssue(
                leftPath,
                "Capabilities must be disjoint between " + leftPath + " and " + rightPath + ", overlap=" + leftValues
            ));
        }
    }

    private static Set<String> normalizeCapabilities(List<String> capabilities) {
        Set<String> normalized = new LinkedHashSet<>();
        for (String capability : capabilities) {
            if (!isBlank(capability)) {
                normalized.add(capability.trim());
            }
        }
        return normalized;
    }

    private static void validateDuplicateModuleId(
        ModuleMetadata metadata,
        Map<String, Integer> moduleIdCounts,
        List<ModuleValidationIssue> issues
    ) {
        if (isBlank(metadata.moduleId())) {
            return;
        }
        if (moduleIdCounts.getOrDefault(metadata.moduleId(), 0) > 1) {
            issues.add(issue(
                ModuleValidationCode.DUPLICATE_MODULE_ID,
                "$.moduleId",
                "Duplicate moduleId detected: " + metadata.moduleId()
            ));
        }
    }

    private static void validateKernelApiCompatibility(
        ModuleMetadata metadata,
        String runningKernelApiVersion,
        List<ModuleValidationIssue> issues
    ) {
        if (isBlank(metadata.requiredKernelApiVersion())) {
            return;
        }
        if (!KernelApiVersionCompatibility.isCompatible(metadata.requiredKernelApiVersion(), runningKernelApiVersion)) {
            issues.add(issue(
                ModuleValidationCode.KERNEL_API_INCOMPATIBLE,
                "$.requiredKernelApiVersion",
                "moduleId '" + metadata.moduleId()
                    + "' declares requiredKernelApiVersion '" + metadata.requiredKernelApiVersion()
                    + "' which is incompatible with running kernel API version '" + runningKernelApiVersion + "'"
            ));
        }
    }

    private static void validateRunningKernelApiVersion(String runningKernelApiVersion) {
        KernelApiVersionCompatibility.validateRunningKernelApiVersion(runningKernelApiVersion);
    }

    private static void required(String value, String fieldPath, List<ModuleValidationIssue> issues) {
        if (isBlank(value)) {
            issues.add(issue(
                ModuleValidationCode.REQUIRED_FIELD_MISSING,
                fieldPath,
                "Required field is missing"
            ));
        }
    }

    private static ModuleValidationIssue issue(ModuleValidationCode code, String fieldPath, String message) {
        return new ModuleValidationIssue(ModuleValidationSeverity.ERROR, code, fieldPath, message);
    }

    private static ModuleValidationIssue capabilityIssue(String fieldPath, String message) {
        return issue(ModuleValidationCode.CAPABILITY_DECLARATION_INVALID, fieldPath, message);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
