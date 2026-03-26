package io.govaryn.kernel.internal;

import io.govaryn.kernel.api.KernelContext;
import io.govaryn.kernel.api.KernelModule;
import io.govaryn.kernel.config.ModuleFailurePolicyAction;
import io.govaryn.kernel.module.ModuleFailureDetails;
import io.govaryn.kernel.module.ModuleLifecycleState;
import io.govaryn.kernel.module.ModuleRegistry;
import io.govaryn.kernel.module.ModuleRegistryEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Component
public class ModuleInitializationExecutor {

    private static final Logger log = LoggerFactory.getLogger(ModuleInitializationExecutor.class);
    private static final int LOG_FIELD_MAX_LENGTH = 512;

    public List<KernelModule> initializeRegisteredModules(
        List<KernelModule> modules,
        KernelContext context,
        ModuleRegistry registry,
        Function<KernelModule, ModuleFailurePolicyAction> failurePolicyResolver
    ) {
        List<KernelModule> initializedModules = new ArrayList<>();

        for (KernelModule module : modules) {
            ModuleFailurePolicyAction failurePolicy = failurePolicyResolver.apply(module);
            String moduleId = module.metadata().moduleId();
            String moduleVersion = module.metadata().moduleVersion();
            String requiredKernelApiVersion = module.metadata().requiredKernelApiVersion();
            Optional<ModuleRegistryEntry> registryEntry = registry.findByModuleId(moduleId);

            if (registryEntry.isEmpty() || registryEntry.get().status().lifecycleState() != ModuleLifecycleState.REGISTERED) {
                log.warn(
                    "event=module_initialization_skipped moduleId={} moduleName={} moduleVersion={} requiredKernelApiVersion={} currentModuleStatus={} errorType={} errorCause={}",
                    sanitizeForLog(moduleId),
                    sanitizeForLog(module.metadata().moduleName()),
                    sanitizeForLog(moduleVersion),
                    sanitizeForLog(requiredKernelApiVersion),
                    sanitizeForLog(registryEntry.map(entry -> entry.status().lifecycleState().name()).orElse("NOT_REGISTERED")),
                    "MODULE_NOT_REGISTERED",
                    "Module is not in REGISTERED state"
                );
                continue;
            }

            try {
                log.info(
                    "event=module_initialization_started moduleId={} moduleName={} moduleVersion={} requiredKernelApiVersion={} currentModuleStatus={}",
                    sanitizeForLog(moduleId),
                    sanitizeForLog(module.metadata().moduleName()),
                    sanitizeForLog(moduleVersion),
                    sanitizeForLog(requiredKernelApiVersion),
                    sanitizeForLog(registryEntry.get().status().lifecycleState().name())
                );
                registry.transitionState(moduleId, ModuleLifecycleState.INITIALIZING, null);
                module.initialize(context);
                registry.transitionState(moduleId, ModuleLifecycleState.INITIALIZED, null);
                log.info(
                    "event=module_initialization_succeeded moduleId={} moduleName={} moduleVersion={} requiredKernelApiVersion={} currentModuleStatus={}",
                    sanitizeForLog(moduleId),
                    sanitizeForLog(module.metadata().moduleName()),
                    sanitizeForLog(moduleVersion),
                    sanitizeForLog(requiredKernelApiVersion),
                    sanitizeForLog(state(registry, moduleId))
                );
                initializedModules.add(module);
            } catch (Exception ex) {
                handleInitializationFailure(module, registry, failurePolicy, ex);
            }
        }

        return List.copyOf(initializedModules);
    }

    private void handleInitializationFailure(
        KernelModule module,
        ModuleRegistry registry,
        ModuleFailurePolicyAction failurePolicy,
        Exception ex
    ) {
        String moduleId = module.metadata().moduleId();
        String moduleName = module.metadata().moduleName();
        String failureMessage = ex.getClass().getSimpleName() + ": " + sanitizeForLog(ex.getMessage());
        ModuleFailureDetails failure = new ModuleFailureDetails("INITIALIZATION_FAILED", failureMessage);

        if (failurePolicy == ModuleFailurePolicyAction.MARK_MODULE_DEGRADED) {
            boolean degraded = safeMarkDegraded(moduleId, registry, failure);
            if (!degraded) {
                safeTransitionToFailed(moduleId, registry, failure);
            }
        } else {
            safeTransitionToFailed(moduleId, registry, failure);
        }

        log.error(
            "event=module_initialization_failed moduleId={} moduleName={} moduleVersion={} requiredKernelApiVersion={} currentModuleStatus={} policy={} errorType={} errorCause={}",
            sanitizeForLog(moduleId),
            sanitizeForLog(moduleName),
            sanitizeForLog(module.metadata().moduleVersion()),
            sanitizeForLog(module.metadata().requiredKernelApiVersion()),
            sanitizeForLog(state(registry, moduleId)),
            failurePolicy,
            ex.getClass().getSimpleName(),
            sanitizeForLog(ex.getMessage()),
            ex
        );

        if (failurePolicy == ModuleFailurePolicyAction.FAIL_FAST) {
            throw new IllegalStateException(
                "Module initialization failed and policy is FAIL_FAST: id=" + moduleId + " name=" + moduleName,
                ex
            );
        }
    }

    private void safeTransitionToFailed(String moduleId, ModuleRegistry registry, ModuleFailureDetails failure) {
        Optional<ModuleRegistryEntry> entry = registry.findByModuleId(moduleId);
        if (entry.isEmpty()) {
            return;
        }

        ModuleLifecycleState state = entry.get().status().lifecycleState();
        try {
            if (state == ModuleLifecycleState.INITIALIZING || state == ModuleLifecycleState.INITIALIZED) {
                registry.transitionState(moduleId, ModuleLifecycleState.FAILED, failure);
                return;
            }

            if (state == ModuleLifecycleState.REGISTERED) {
                registry.transitionState(moduleId, ModuleLifecycleState.INITIALIZING, null);
                registry.transitionState(moduleId, ModuleLifecycleState.FAILED, failure);
            }
        } catch (Exception transitionError) {
            log.warn(
                "event=module_transition_failed moduleId={} currentModuleStatus={} errorType={} errorCause={}",
                sanitizeForLog(moduleId),
                sanitizeForLog(state(registry, moduleId)),
                transitionError.getClass().getSimpleName(),
                sanitizeForLog(transitionError.getMessage())
            );
        }
    }

    private boolean safeMarkDegraded(String moduleId, ModuleRegistry registry, ModuleFailureDetails failure) {
        try {
            registry.markDegraded(moduleId, failure);
            return true;
        } catch (Exception markError) {
            log.warn(
                "event=module_mark_degraded_failed moduleId={} currentModuleStatus={} errorType={} errorCause={}",
                sanitizeForLog(moduleId),
                sanitizeForLog(state(registry, moduleId)),
                markError.getClass().getSimpleName(),
                sanitizeForLog(markError.getMessage())
            );
            return false;
        }
    }

    private String state(ModuleRegistry registry, String moduleId) {
        return registry.findByModuleId(moduleId)
            .map(entry -> entry.status().lifecycleState().name())
            .orElse("NOT_REGISTERED");
    }

    private static String sanitizeForLog(String value) {
        if (value == null) {
            return "null";
        }
        String sanitized = value.replaceAll("[\\r\\n\\t\\x00-\\x1F]", " ").trim();
        if (sanitized.length() <= LOG_FIELD_MAX_LENGTH) {
            return sanitized;
        }
        return sanitized.substring(0, LOG_FIELD_MAX_LENGTH) + "...";
    }
}
