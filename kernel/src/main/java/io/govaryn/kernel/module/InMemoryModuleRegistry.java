package io.govaryn.kernel.module;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Component
public class InMemoryModuleRegistry implements ModuleRegistry {

    private static final Pattern SEMVER_PATTERN = Pattern.compile(
        "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*)?(?:\\+[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*)?$"
    );
    private final Map<String, ModuleRegistryEntry> entries = new ConcurrentHashMap<>();

    @Override
    public void addDiscovered(ModuleMetadata metadata, boolean mandatory, String origin) {
        ModuleRegistryEntry entry = new ModuleRegistryEntry(metadata, ModuleStatus.discovered(), mandatory, origin);
        ModuleRegistryEntry previous = entries.putIfAbsent(metadata.moduleId(), entry);
        if (previous != null) {
            throw new IllegalStateException(
                "Duplicate moduleId detected: " + metadata.moduleId()
                    + " (existing: " + previous.metadata().moduleName()
                    + "@" + previous.origin()
                    + ", incoming: " + metadata.moduleName()
                    + "@" + origin + ")"
            );
        }
    }

    @Override
    public void registerValidated(ModuleMetadata metadata, boolean mandatory, String origin, String runningKernelApiVersion) {
        ensureKernelApiCompatibility(metadata, runningKernelApiVersion);
        addDiscovered(metadata, mandatory, origin);
        transitionState(metadata.moduleId(), ModuleLifecycleState.VALIDATED, null);
        transitionState(metadata.moduleId(), ModuleLifecycleState.REGISTERED, null);
    }

    @Override
    public Optional<ModuleRegistryEntry> findByModuleId(String moduleId) {
        return Optional.ofNullable(entries.get(moduleId));
    }

    @Override
    public List<ModuleRegistryEntry> findAll() {
        return entries.values().stream()
            .sorted(Comparator.comparing(entry -> entry.metadata().moduleId()))
            .toList();
    }

    @Override
    public void transitionState(String moduleId, ModuleLifecycleState targetState, ModuleFailureDetails errorDetails) {
        entries.compute(moduleId, (id, current) -> {
            if (current == null) {
                throw new IllegalArgumentException("Unknown moduleId: " + moduleId);
            }

            ModuleLifecycleState sourceState = current.status().lifecycleState();
            if (!ModuleLifecycleTransitions.canTransition(sourceState, targetState)) {
                throw new IllegalStateException("Invalid state transition: " + sourceState + " -> " + targetState);
            }

            return current.withStatus(ModuleStatus.withState(targetState, errorDetails));
        });
    }

    @Override
    public void markDegraded(String moduleId, ModuleFailureDetails errorDetails) {
        entries.compute(moduleId, (id, current) -> {
            if (current == null) {
                throw new IllegalArgumentException("Unknown moduleId: " + moduleId);
            }
            ModuleLifecycleState state = current.status().lifecycleState();
            if (state != ModuleLifecycleState.INITIALIZING && state != ModuleLifecycleState.INITIALIZED) {
                throw new IllegalStateException("Degraded mode only allowed for initializing or initialized modules");
            }
            if (!ModuleLifecycleTransitions.canTransition(state, ModuleLifecycleState.DEGRADED)) {
                throw new IllegalStateException("Invalid state transition: " + state + " -> " + ModuleLifecycleState.DEGRADED);
            }
            return current.withStatus(ModuleStatus.degraded(errorDetails));
        });
    }

    @Override
    public void remove(String moduleId) {
        entries.remove(moduleId);
    }

    private static void ensureKernelApiCompatibility(ModuleMetadata metadata, String runningKernelApiVersion) {
        if (runningKernelApiVersion == null || runningKernelApiVersion.isBlank()) {
            throw new IllegalStateException("runningKernelApiVersion must not be blank");
        }
        if (!isKernelVersionCompatible(metadata.requiredKernelApiVersion(), runningKernelApiVersion)) {
            throw new IllegalStateException(
                "Cannot register moduleId '" + metadata.moduleId()
                    + "': requiredKernelApiVersion '" + metadata.requiredKernelApiVersion()
                    + "' is incompatible with running kernel API version '" + runningKernelApiVersion + "'"
            );
        }
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
