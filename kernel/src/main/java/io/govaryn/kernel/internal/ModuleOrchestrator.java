package io.govaryn.kernel.internal;

import io.govaryn.kernel.api.KernelContext;
import io.govaryn.kernel.api.KernelModule;
import io.govaryn.kernel.config.GovarynKernelProperties;
import io.govaryn.kernel.config.ModuleFailurePolicyAction;
import io.govaryn.kernel.module.ModuleFailureDetails;
import io.govaryn.kernel.module.ModuleLifecycleState;
import io.govaryn.kernel.module.ModuleRegistry;
import io.govaryn.kernel.module.ModuleRegistryEntry;
import io.govaryn.kernel.module.discovery.ModuleDiscoveryCandidate;
import io.govaryn.kernel.module.discovery.ModuleDiscoveryService;
import io.govaryn.kernel.module.identity.ModuleIdentityCollision;
import io.govaryn.kernel.module.identity.ModuleIdentityCollisionDetector;
import io.govaryn.kernel.module.validation.ModuleValidationIssue;
import io.govaryn.kernel.module.validation.ModuleValidationReport;
import io.govaryn.kernel.module.validation.ModuleValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ModuleOrchestrator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ModuleOrchestrator.class);

    private final List<KernelModule> modules;
    private final GovarynKernelProperties properties;
    private final String kernelVersion;
    private final ModuleDiscoveryService discoveryService;
    private final ModuleValidator moduleValidator;
    private final ModuleRegistry moduleRegistry;
    private final ModuleIdentityCollisionDetector collisionDetector;
    private final ModuleInitializationExecutor initializationExecutor;

    public ModuleOrchestrator(ObjectProvider<KernelModule> modules,
                              GovarynKernelProperties properties,
                              ModuleDiscoveryService discoveryService,
                              ModuleValidator moduleValidator,
                              ModuleRegistry moduleRegistry,
                              ModuleIdentityCollisionDetector collisionDetector,
                              ModuleInitializationExecutor initializationExecutor,
                              @Value("${spring.application.version:${project.version:unknown}}") String kernelVersion) {
        this.modules = modules.orderedStream().sorted(Comparator.comparingInt(KernelModule::order)).toList();
        this.properties = properties;
        this.discoveryService = discoveryService;
        this.moduleValidator = moduleValidator;
        this.moduleRegistry = moduleRegistry;
        this.collisionDetector = collisionDetector;
        this.initializationExecutor = initializationExecutor;
        this.kernelVersion = kernelVersion;
    }

    @Override
    public void run(ApplicationArguments args) {
        KernelContext context = new KernelContext(properties.getId(), properties.getEnvironment(), kernelVersion);
        List<ModuleDiscoveryCandidate> candidates = discoveryService.discover();
        List<ModuleValidationReport> reports = moduleValidator.validate(candidates, kernelVersion);

        log.info("Discovered {} module candidate(s) at startup", candidates.size());
        for (ModuleDiscoveryCandidate candidate : candidates) {
            log.info(
                "event=module_discovered moduleId={} moduleName={} moduleVersion={} requiredKernelApiVersion={} currentModuleStatus={} source={} origin={} loadable={}",
                sanitizeForLog(candidate.metadata().moduleId()),
                sanitizeForLog(candidate.metadata().moduleName()),
                sanitizeForLog(candidate.metadata().moduleVersion()),
                sanitizeForLog(candidate.metadata().requiredKernelApiVersion()),
                sanitizeForLog(currentModuleStatus(candidate.metadata().moduleId())),
                candidate.source(),
                sanitizeForLog(candidate.origin()),
                candidate.loadableInCurrentRuntime()
            );
        }

        List<ModuleIdentityCollision> collisions = collisionDetector.detect(candidates, moduleRegistry);
        if (!collisions.isEmpty()) {
            throw new IllegalStateException(collisionDetector.formatCollisionMessage(collisions));
        }

        Map<String, ModuleDiscoveryCandidate> candidatesById = new LinkedHashMap<>();
        for (ModuleDiscoveryCandidate candidate : candidates) {
            candidatesById.put(reportKey(candidate.metadata().moduleId(), candidate.source(), candidate.origin()), candidate);
        }

        for (ModuleValidationReport report : reports) {
            ModuleDiscoveryCandidate candidate = candidatesById.get(reportKey(report.moduleId(), report.source(), report.origin()));
            String moduleVersion = candidate != null ? candidate.metadata().moduleVersion() : "unknown";
            String requiredKernelApiVersion = candidate != null ? candidate.metadata().requiredKernelApiVersion() : "unknown";
            String status = report.valid() ? "VALIDATED" : "REJECTED";
            log.info(
                "event=module_validation_report moduleId={} moduleName={} moduleVersion={} requiredKernelApiVersion={} currentModuleStatus={} source={} valid={} issueCount={}",
                sanitizeForLog(report.moduleId()),
                sanitizeForLog(report.moduleName()),
                sanitizeForLog(moduleVersion),
                sanitizeForLog(requiredKernelApiVersion),
                sanitizeForLog(status),
                report.source(),
                report.valid(),
                report.issues().size()
            );
            for (ModuleValidationIssue issue : report.issues()) {
                log.warn(
                    "event=module_validation_issue moduleId={} moduleVersion={} requiredKernelApiVersion={} currentModuleStatus={} errorType={} errorCause={} severity={} field={}",
                    sanitizeForLog(report.moduleId()),
                    sanitizeForLog(moduleVersion),
                    sanitizeForLog(requiredKernelApiVersion),
                    sanitizeForLog(status),
                    issue.code(),
                    sanitizeForLog(issue.message()),
                    issue.severity(),
                    sanitizeForLog(issue.fieldPath())
                );
            }
        }

        Map<String, ModuleValidationReport> reportsByCompositeKey = reports.stream()
            .collect(Collectors.toMap(
                report -> reportKey(report.moduleId(), report.source(), report.origin()),
                Function.identity(),
                (first, ignored) -> first,
                LinkedHashMap::new
            ));

        Map<String, ModuleDiscoveryCandidate> validCandidatesById = new LinkedHashMap<>();
        for (ModuleDiscoveryCandidate candidate : candidates) {
            ModuleValidationReport report = reportsByCompositeKey.get(
                reportKey(candidate.metadata().moduleId(), candidate.source(), candidate.origin())
            );
            if (report == null) {
                log.warn(
                    "Missing validation report for moduleId={} source={} origin={}; candidate is treated as invalid",
                    sanitizeForLog(candidate.metadata().moduleId()),
                    candidate.source(),
                    sanitizeForLog(candidate.origin())
                );
                continue;
            }
            if (report.valid()) {
                validCandidatesById.put(candidate.metadata().moduleId(), candidate);
            }
        }

        for (ModuleDiscoveryCandidate validCandidate : validCandidatesById.values()) {
            moduleRegistry.registerValidated(validCandidate.metadata(), false, validCandidate.origin());
            log.info(
                "event=module_registered moduleId={} moduleName={} moduleVersion={} requiredKernelApiVersion={} currentModuleStatus={} origin={}",
                sanitizeForLog(validCandidate.metadata().moduleId()),
                sanitizeForLog(validCandidate.metadata().moduleName()),
                sanitizeForLog(validCandidate.metadata().moduleVersion()),
                sanitizeForLog(validCandidate.metadata().requiredKernelApiVersion()),
                sanitizeForLog(currentModuleStatus(validCandidate.metadata().moduleId())),
                sanitizeForLog(validCandidate.origin())
            );
        }

        int rejectedCandidates = candidates.size() - validCandidatesById.size();
        if (rejectedCandidates > 0) {
            log.warn("Rejected {} invalid module candidate(s); they were not added to the registry", rejectedCandidates);
        }

        Set<String> validLoadableModuleIds = new HashSet<>();
        for (ModuleDiscoveryCandidate candidate : validCandidatesById.values()) {
            if (candidate.loadableInCurrentRuntime()) {
                validLoadableModuleIds.add(candidate.metadata().moduleId());
            }
        }

        List<KernelModule> modulesToStart = modules.stream()
            .filter(module -> validLoadableModuleIds.contains(module.metadata().moduleId()))
            .toList();

        int skipped = modules.size() - modulesToStart.size();
        if (skipped > 0) {
            log.warn("Skipping {} module(s) due to validation errors or non-loadable discovery state", skipped);
        }

        List<KernelModule> initializedModules = initializationExecutor.initializeRegisteredModules(
            modulesToStart,
            context,
            moduleRegistry,
            properties.getModuleInitializationFailurePolicy()
        );

        for (KernelModule module : initializedModules) {
            log.info(
                "event=module_starting moduleId={} moduleName={} moduleVersion={} requiredKernelApiVersion={} currentModuleStatus={}",
                sanitizeForLog(module.metadata().moduleId()),
                sanitizeForLog(module.metadata().moduleName()),
                sanitizeForLog(module.metadata().moduleVersion()),
                sanitizeForLog(module.metadata().requiredKernelApiVersion()),
                sanitizeForLog(currentModuleStatus(module.metadata().moduleId()))
            );
            try {
                module.start();
            } catch (Exception ex) {
                handleStartFailure(module, ex);
            }
        }

        List<ModuleRegistryEntry> registryEntries = moduleRegistry.findAll();
        int foundCount = candidates.size();
        int validatedCount = validCandidatesById.size();
        int rejectedCount = foundCount - validatedCount;
        int registeredCount = registryEntries.size();
        int failedCount = (int) registryEntries.stream()
            .filter(entry -> entry.status().lifecycleState() == ModuleLifecycleState.FAILED)
            .count();
        int degradedCount = (int) registryEntries.stream()
            .filter(entry -> entry.status().lifecycleState() == ModuleLifecycleState.DEGRADED || entry.status().degraded())
            .count();

        log.info(
            "event=module_startup_summary found={} validated={} rejected={} registered={} failed={} degraded={}",
            foundCount,
            validatedCount,
            rejectedCount,
            registeredCount,
            failedCount,
            degradedCount
        );

        log.info("Govaryn Kernel started with {} module(s)", initializedModules.size());
    }

    private String currentModuleStatus(String moduleId) {
        Optional<ModuleRegistryEntry> entry = moduleRegistry.findByModuleId(moduleId);
        return entry.map(value -> value.status().lifecycleState().name()).orElse("NOT_REGISTERED");
    }

    private void handleStartFailure(KernelModule module, Exception ex) {
        String moduleId = module.metadata().moduleId();
        ModuleFailureDetails failureDetails = new ModuleFailureDetails(
            "START_FAILED",
            ex.getClass().getSimpleName() + ": " + sanitizeForLog(ex.getMessage())
        );
        ModuleFailurePolicyAction policy = properties.getModuleInitializationFailurePolicy();
        try {
            if (policy == ModuleFailurePolicyAction.MARK_MODULE_DEGRADED) {
                moduleRegistry.markDegraded(moduleId, failureDetails);
            } else {
                moduleRegistry.transitionState(moduleId, ModuleLifecycleState.FAILED, failureDetails);
            }
        } catch (Exception transitionError) {
            log.warn(
                "event=module_start_transition_failed moduleId={} currentModuleStatus={} errorType={} errorCause={}",
                sanitizeForLog(moduleId),
                sanitizeForLog(currentModuleStatus(moduleId)),
                transitionError.getClass().getSimpleName(),
                sanitizeForLog(transitionError.getMessage())
            );
        }

        log.error(
            "event=module_start_failed moduleId={} moduleName={} currentModuleStatus={} policy={} errorType={} errorCause={}",
            sanitizeForLog(moduleId),
            sanitizeForLog(module.metadata().moduleName()),
            sanitizeForLog(currentModuleStatus(moduleId)),
            policy,
            ex.getClass().getSimpleName(),
            sanitizeForLog(ex.getMessage()),
            ex
        );

        if (policy == ModuleFailurePolicyAction.FAIL_FAST) {
            throw new IllegalStateException(
                "Module start failed and policy is FAIL_FAST: id=" + moduleId + " name=" + module.metadata().moduleName(),
                ex
            );
        }
    }

    private static String reportKey(String moduleId, Object source, String origin) {
        return String.join("|",
            sanitizeForKey(moduleId),
            sanitizeForKey(Objects.toString(source, "unknown")),
            sanitizeForKey(origin)
        );
    }

    private static String sanitizeForKey(String value) {
        if (value == null) {
            return "null";
        }
        return value.replace("|", "\\|").trim();
    }

    private static String sanitizeForLog(String value) {
        if (value == null) {
            return "null";
        }
        return value.replaceAll("[\\r\\n\\t\\x00-\\x1F]", " ").trim();
    }
}
