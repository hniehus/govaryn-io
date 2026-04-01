package io.govaryn.kernel.health.security;

import io.govaryn.kernel.security.authorization.framework.AuthorizationScopeExtractor;
import io.govaryn.kernel.security.authorization.framework.ModuleSecurityContributor;
import io.govaryn.kernel.security.authorization.framework.ModuleSecurityRegistry;
import io.govaryn.kernel.security.authorization.framework.model.AuthorizationAction;
import io.govaryn.kernel.security.authorization.framework.model.AuthorizationDecision;
import io.govaryn.kernel.security.authorization.framework.model.AuthorizationRequest;
import io.govaryn.kernel.security.authorization.framework.model.DenyReason;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

/**
 * Security contribution for module status endpoint access.
 */
@Component
public class ModuleStatusSecurityContributor implements ModuleSecurityContributor {

    private static final String POLICY_SOURCE = "kernel.module-status.scope-policy";

    @Override
    public String moduleId() {
        return ModuleStatusAuthorizationContract.MODULE_ID;
    }

    @Override
    public void contribute(ModuleSecurityRegistry registry) {
        registry.registerResourcePolicy(
            ModuleStatusAuthorizationContract.RESOURCE_TYPE,
            EnumSet.of(AuthorizationAction.READ),
            this::evaluatePolicy
        );
    }

    private AuthorizationDecision evaluatePolicy(AuthorizationRequest request) {
        Set<String> scopes = AuthorizationScopeExtractor.extractScopes(request);
        if (scopes.contains(ModuleStatusAuthorizationContract.SCOPE_READ)) {
            return AuthorizationDecision.allow(POLICY_SOURCE);
        }
        return AuthorizationDecision.deny(DenyReason.RESOURCE_ACCESS_DENIED, POLICY_SOURCE);
    }
}
