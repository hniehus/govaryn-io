package io.govaryn.kernel.security.authorization.framework;

import io.govaryn.kernel.security.KernelRequestSecurityContext;
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

    private final KernelRequestSecurityContext requestSecurityContext;
    private final AuthorizationService authorizationService;

    public KernelAuthorizationEnforcer(
        KernelRequestSecurityContext requestSecurityContext,
        AuthorizationService authorizationService
    ) {
        this.requestSecurityContext = requestSecurityContext;
        this.authorizationService = authorizationService;
    }

    public void enforce(
        String moduleId,
        AuthorizationAction action,
        String resourceType,
        String resourceId,
        Map<String, String> attributes
    ) {
        SecurityContext securityContext = requestSecurityContext.current()
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
}
