package io.govaryn.kernel.module.validation;

import io.govaryn.kernel.module.ModuleMetadata;
import io.govaryn.kernel.module.discovery.ModuleDiscoveryCandidate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
public class DefaultModuleValidator implements ModuleValidator {

    private static final Pattern MODULE_ID_PATTERN = Pattern.compile("^[a-z][a-z0-9-]{2,63}$");
    private static final Pattern SEMVER_PATTERN = Pattern.compile(
        "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*)?(?:\\+[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*)?$"
    );
    private static final String SUPPORTED_CONTRACT_VERSION = "1.0.0";

    @Override
    public List<ModuleValidationReport> validate(List<ModuleDiscoveryCandidate> candidates, String runningKernelApiVersion) {
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
            validateDuplicateModuleId(metadata, moduleIdCounts, issues);
            validateKernelApiCompatibility(metadata, runningKernelApiVersion, issues);

            boolean valid = issues.stream().noneMatch(issue -> issue.severity() == ModuleValidationSeverity.ERROR);
            reports.add(new ModuleValidationReport(
                metadata.moduleId(),
                metadata.moduleName(),
                candidate.source(),
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

        if (!isBlank(metadata.requiredKernelApiVersion()) && !isSupportedVersionRange(metadata.requiredKernelApiVersion())) {
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
        if (isBlank(metadata.requiredKernelApiVersion()) || isBlank(runningKernelApiVersion)) {
            return;
        }
        if (!isKernelVersionCompatible(metadata.requiredKernelApiVersion(), runningKernelApiVersion)) {
            issues.add(issue(
                ModuleValidationCode.KERNEL_API_INCOMPATIBLE,
                "$.requiredKernelApiVersion",
                "requiredKernelApiVersion '" + metadata.requiredKernelApiVersion()
                    + "' is incompatible with running kernel version '" + runningKernelApiVersion + "'"
            ));
        }
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

    private static boolean isBlank(String value) {
        return Objects.isNull(value) || value.isBlank();
    }

    private static boolean isSupportedVersionRange(String range) {
        String trimmed = range.trim();
        if ("*".equals(trimmed)) {
            return true;
        }
        if (trimmed.startsWith("^")) {
            return SEMVER_PATTERN.matcher(trimmed.substring(1)).matches();
        }
        if (trimmed.startsWith(">=")) {
            return SEMVER_PATTERN.matcher(trimmed.substring(2).trim()).matches();
        }
        return SEMVER_PATTERN.matcher(trimmed).matches();
    }

    private static boolean isKernelVersionCompatible(String requiredRange, String runningVersionRaw) {
        String runningVersion = normalizeVersion(runningVersionRaw);
        if (!SEMVER_PATTERN.matcher(runningVersion).matches()) {
            return false;
        }

        String trimmed = requiredRange.trim();
        if ("*".equals(trimmed)) {
            return true;
        }
        if (trimmed.startsWith("^")) {
            String base = trimmed.substring(1).trim();
            if (!SEMVER_PATTERN.matcher(base).matches()) {
                return false;
            }
            if (compareSemver(runningVersion, base) < 0) {
                return false;
            }
            int[] baseCore = parseSemverCore(base);
            int[] runningCore = parseSemverCore(runningVersion);
            if (baseCore[0] > 0) {
                return baseCore[0] == runningCore[0];
            }
            if (baseCore[1] > 0) {
                return runningCore[0] == 0 && runningCore[1] == baseCore[1];
            }
            return runningCore[0] == 0 && runningCore[1] == 0 && runningCore[2] == baseCore[2];
        }
        if (trimmed.startsWith(">=")) {
            String minimum = trimmed.substring(2).trim();
            if (!SEMVER_PATTERN.matcher(minimum).matches()) {
                return false;
            }
            return compareSemver(runningVersion, minimum) >= 0;
        }
        if (!SEMVER_PATTERN.matcher(trimmed).matches()) {
            return false;
        }
        return compareSemver(runningVersion, trimmed) == 0;
    }

    private static String normalizeVersion(String version) {
        String trimmed = version.trim();
        if (SEMVER_PATTERN.matcher(trimmed).matches()) {
            return trimmed;
        }
        int idx = trimmed.indexOf('-');
        String noQualifier = idx > 0 ? trimmed.substring(0, idx) : trimmed;
        return SEMVER_PATTERN.matcher(noQualifier).matches() ? noQualifier : trimmed;
    }

    private static int compareSemver(String left, String right) {
        int[] l = parseSemverCore(left);
        int[] r = parseSemverCore(right);

        if (l[0] != r[0]) return Integer.compare(l[0], r[0]);
        if (l[1] != r[1]) return Integer.compare(l[1], r[1]);
        return Integer.compare(l[2], r[2]);
    }

    private static int[] parseSemverCore(String version) {
        String core = version.split("[-+]")[0];
        String[] parts = core.split("\\.");
        return new int[]{
            Integer.parseInt(parts[0]),
            Integer.parseInt(parts[1]),
            Integer.parseInt(parts[2])
        };
    }
}
