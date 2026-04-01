package io.govaryn.kernel.security.authorization.framework;

import io.govaryn.kernel.security.authorization.framework.model.AuthorizationDecision;
import io.govaryn.kernel.security.authorization.framework.model.AuthorizationRequest;
import io.govaryn.kernel.security.authorization.framework.model.DenyReason;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Central kernel authorization orchestrator for module-provided resource policy evaluators.
 */
@Service
public class KernelAuthorizationService implements AuthorizationService {

    private final ResourcePolicyRegistry resourcePolicyRegistry;
    private final AuthorizationAuditLogger auditLogger;

    public KernelAuthorizationService(
        ResourcePolicyRegistry resourcePolicyRegistry,
        AuthorizationAuditLogger auditLogger
    ) {
        this.resourcePolicyRegistry = resourcePolicyRegistry;
        this.auditLogger = auditLogger;
    }

    @Override
    public AuthorizationDecision authorize(AuthorizationRequest request) {
        if (request == null) {
            return denyAndAudit(null, DenyReason.INVALID_REQUEST, "kernel.authorization", null);
        }

        Optional<ResourcePolicyRegistration> registration = resourcePolicyRegistry.resolve(
            request.moduleId(),
            request.resourceType()
        );
        if (registration.isEmpty()) {
            return denyAndAudit(
                request,
                DenyReason.MODULE_SECURITY_NOT_REGISTERED,
                "kernel.registry",
                null
            );
        }

        ResourcePolicyRegistration policy = registration.get();
        if (!policy.supportedActions().contains(request.action())) {
            return denyAndAudit(
                request,
                DenyReason.RESOURCE_ACCESS_DENIED,
                policySource(policy),
                null
            );
        }

        try {
            AuthorizationDecision evaluated = policy.evaluator().evaluate(request);
            AuthorizationDecision normalized = normalizeDecision(evaluated, policy);
            if (!normalized.allowed()) {
                auditLogger.logDenied(request, normalized, null);
            }
            return normalized;
        } catch (Exception ex) {
            return denyAndAudit(request, DenyReason.EVALUATION_ERROR, policySource(policy), ex);
        }
    }

    private AuthorizationDecision normalizeDecision(
        AuthorizationDecision evaluated,
        ResourcePolicyRegistration policy
    ) {
        if (evaluated == null) {
            return AuthorizationDecision.deny(DenyReason.EVALUATION_ERROR, policySource(policy));
        }

        String source = hasText(evaluated.policySource()) ? evaluated.policySource() : policySource(policy);
        if (evaluated.allowed()) {
            return AuthorizationDecision.allow(source);
        }

        DenyReason denyReason = evaluated.denyReason() == null ? DenyReason.EVALUATION_ERROR : evaluated.denyReason();
        return AuthorizationDecision.deny(denyReason, source);
    }

    private AuthorizationDecision denyAndAudit(
        AuthorizationRequest request,
        DenyReason reason,
        String policySource,
        Throwable evaluationError
    ) {
        AuthorizationDecision decision = AuthorizationDecision.deny(reason, policySource);
        auditLogger.logDenied(request, decision, evaluationError);
        return decision;
    }

    private String policySource(ResourcePolicyRegistration policy) {
        return policy.moduleId() + ":" + policy.resourceType();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
