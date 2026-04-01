package io.govaryn.modules.examples;

import io.govaryn.kernel.api.KernelContext;
import io.govaryn.kernel.api.KernelModule;
import io.govaryn.kernel.module.ModuleMetadata;
import io.govaryn.kernel.module.ModuleType;
import io.govaryn.kernel.security.authorization.framework.AuthorizationScopeExtractor;
import io.govaryn.kernel.security.authorization.framework.ModuleSecurityContributor;
import io.govaryn.kernel.security.authorization.framework.ModuleSecurityRegistry;
import io.govaryn.kernel.security.authorization.framework.model.AuthorizationAction;
import io.govaryn.kernel.security.authorization.framework.model.AuthorizationDecision;
import io.govaryn.kernel.security.authorization.framework.model.AuthorizationRequest;
import io.govaryn.kernel.security.authorization.framework.model.DenyReason;

import java.util.EnumSet;
import java.util.Set;

/**
 * Minimal reference module that contributes a tenant/scope-based resource policy.
 */
public class ReferenceAuthorizationModule implements KernelModule, ModuleSecurityContributor {

    private static final String POLICY_SOURCE = "reference.authorization.tenant-scope-policy";

    @Override
    public ModuleMetadata metadata() {
        return ModuleMetadata.minimal(
            ReferenceAuthorizationContract.MODULE_ID,
            "Reference Authorization Module",
            "1.0.0",
            "^1.0.0",
            ModuleType.FEATURE,
            getClass().getName()
        );
    }

    @Override
    public void initialize(KernelContext context) {
        // Intentionally minimal no-op initialization.
    }

    @Override
    public String moduleId() {
        return ReferenceAuthorizationContract.MODULE_ID;
    }

    @Override
    public void contribute(ModuleSecurityRegistry registry) {
        registry.registerResourcePolicy(
            ReferenceAuthorizationContract.RESOURCE_TYPE,
            EnumSet.of(AuthorizationAction.READ, AuthorizationAction.LIST, AuthorizationAction.UPDATE),
            this::evaluatePolicy
        );
    }

    private AuthorizationDecision evaluatePolicy(AuthorizationRequest request) {
        String requestTenantId = request.attributes().get(ReferenceAuthorizationContract.ATTRIBUTE_TENANT_ID);
        String contextTenantId = request.securityContext().tenantId();
        if (!hasText(requestTenantId) || !hasText(contextTenantId) || !requestTenantId.equals(contextTenantId)) {
            return AuthorizationDecision.deny(DenyReason.RECORD_ACCESS_DENIED, POLICY_SOURCE);
        }

        Set<String> scopes = AuthorizationScopeExtractor.extractScopes(request);
        return switch (request.action()) {
            case READ, LIST -> canRead(scopes)
                ? AuthorizationDecision.allow(POLICY_SOURCE)
                : AuthorizationDecision.deny(DenyReason.RESOURCE_ACCESS_DENIED, POLICY_SOURCE);
            case UPDATE -> scopes.contains(ReferenceAuthorizationContract.SCOPE_WRITE)
                ? AuthorizationDecision.allow(POLICY_SOURCE)
                : AuthorizationDecision.deny(DenyReason.RECORD_ACCESS_DENIED, POLICY_SOURCE);
            case CREATE, DELETE -> AuthorizationDecision.deny(DenyReason.RESOURCE_ACCESS_DENIED, POLICY_SOURCE);
        };
    }

    private boolean canRead(Set<String> scopes) {
        return scopes.contains(ReferenceAuthorizationContract.SCOPE_READ)
            || scopes.contains(ReferenceAuthorizationContract.SCOPE_WRITE);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
