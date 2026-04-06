package io.govaryn.kernel.security.authorization.framework;

import io.govaryn.kernel.api.KernelCurrentSecurityContext;
import io.govaryn.kernel.security.KernelSecurityTenantContext;
import io.govaryn.kernel.security.authorization.framework.model.AuthorizationAction;
import io.govaryn.kernel.security.authorization.framework.model.AuthorizationDecision;
import io.govaryn.kernel.security.authorization.framework.model.AuthorizationRequest;
import io.govaryn.kernel.security.authorization.framework.model.DenyReason;
import io.govaryn.kernel.security.authorization.framework.model.SecurityContext;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Explicit kernel-controlled authorization enforcement hook for standard backend paths.
 */
@Component
public class KernelAuthorizationEnforcer {

    private final KernelCurrentSecurityContext currentSecurityContext;
    private final AuthorizationService authorizationService;

    public KernelAuthorizationEnforcer(
        KernelCurrentSecurityContext currentSecurityContext,
        AuthorizationService authorizationService
    ) {
        this.currentSecurityContext = currentSecurityContext;
        this.authorizationService = authorizationService;
    }

    public void enforce(
        String moduleId,
        AuthorizationAction action,
        String resourceType,
        String resourceId,
        Map<String, String> attributes
    ) {
        SecurityContext securityContext = currentSecurityContext.currentKernelContext()
            .map(this::toAuthorizationSecurityContext)
            .orElseThrow(() -> new KernelAccessDeniedException(
                DenyReason.SUBJECT_NOT_AUTHENTICATED,
                moduleId,
                resourceType,
                action,
                resourceId
            ));

        AuthorizationRequest request;
        try {
            request = new AuthorizationRequest(
                securityContext,
                moduleId,
                action,
                resourceType,
                resourceId,
                attributes == null ? Map.of() : attributes
            );
        } catch (IllegalArgumentException ex) {
            throw new KernelAccessDeniedException(
                DenyReason.INVALID_REQUEST,
                moduleId,
                resourceType,
                action,
                resourceId,
                ex
            );
        }

        AuthorizationDecision decision = authorizationService.authorize(request);
        if (!decision.allowed()) {
            throw new KernelAccessDeniedException(
                decision.denyReason(),
                moduleId,
                resourceType,
                action,
                resourceId
            );
        }
    }

    private SecurityContext toAuthorizationSecurityContext(KernelSecurityTenantContext kernelContext) {
        return new SecurityContext(
            kernelContext.userId(),
            kernelContext.activeTenantId(),
            kernelContext.authorities(),
            kernelContext.claims(),
            kernelContext.authenticationMetadata()
        );
    }
}
