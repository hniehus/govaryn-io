package io.govaryn.modules.examples;

import io.govaryn.kernel.api.KernelContext;
import io.govaryn.kernel.api.KernelAuthorizationService;
import io.govaryn.kernel.api.KernelModule;
import io.govaryn.kernel.api.KernelAuthorizationOperations;
import io.govaryn.kernel.module.ModuleCapabilities;
import io.govaryn.kernel.module.ModuleFailurePolicy;
import io.govaryn.kernel.module.ModuleMetadata;
import io.govaryn.kernel.module.ModuleType;
import io.govaryn.kernel.security.authorization.model.AuthorizationDecisionResult;
import io.govaryn.kernel.security.authorization.model.AuthorizationSubject;

import java.util.List;
import java.util.Map;

/**
 * Production-like reference module built on top of the module template.
 * Demonstrates realistic metadata and capability usage while staying small.
 */
public class ReferenceFeatureModule implements KernelModule {

    private final boolean failDuringInitialize;
    private KernelAuthorizationService authorizationService;

    public ReferenceFeatureModule() {
        this(false);
    }

    public ReferenceFeatureModule(boolean failDuringInitialize) {
        this.failDuringInitialize = failDuringInitialize;
    }

    @Override
    public ModuleMetadata metadata() {
        return new ModuleMetadata(
            "1.0.0",
            "reference-feature",
            "Reference Feature Module",
            "1.0.0",
            "^1.0.0",
            ModuleType.FEATURE,
            getClass().getName(),
            "Production-like reference module for kernel integration patterns",
            "Govaryn",
            "Apache-2.0",
            "https://govaryn.io/modules/reference-feature",
            new ModuleCapabilities(
                List.of("reference.feature.sample"),
                List.of("reference.platform.config"),
                List.of("reference.platform.metrics")
            ),
            ModuleFailurePolicy.defaults(),
            null,
            List.of()
        );
    }

    @Override
    public void initialize(KernelContext context) {
        this.authorizationService = context.authorizationService();

        // Controlled failure path for integration tests and diagnostics.
        if (failDuringInitialize) {
            throw new IllegalStateException(
                "Reference feature module forced initialization failure for testing"
            );
        }
    }

    public String restartProtectedModule(AuthorizationSubject subject, String targetModuleId, String environment) {
        if (authorizationService == null) {
            throw new IllegalStateException("KernelAuthorizationService is not available in KernelContext");
        }

        var operation = KernelAuthorizationOperations.of(
            "restart",
            "module",
            targetModuleId,
            Map.of("environment", environment)
        );
        var decision = authorizationService.authorize(subject, operation);
        if (decision.result() != AuthorizationDecisionResult.PERMIT) {
            throw new SecurityException(
                "Protected capability denied: rule=" + decision.matchedRuleId() + " reason=" + decision.reasonCode()
            );
        }

        return "module-restart-requested:" + targetModuleId;
    }
}
