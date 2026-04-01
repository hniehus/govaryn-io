package io.govaryn.kernel.backend.standard;

import io.govaryn.kernel.security.authorization.framework.ModuleSecurityContributor;
import io.govaryn.kernel.security.authorization.framework.ModuleSecurityRegistry;
import io.govaryn.kernel.security.authorization.framework.model.AuthorizationAction;
import io.govaryn.kernel.security.authorization.framework.model.AuthorizationDecision;
import io.govaryn.kernel.security.authorization.framework.model.AuthorizationRequest;
import io.govaryn.kernel.security.authorization.framework.model.DenyReason;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class KernelStandardRecordSecurityContributor implements ModuleSecurityContributor {

    private static final Pattern SCOPE_SPLIT_PATTERN = Pattern.compile("[,\\s]+");
    private static final String POLICY_SOURCE = "kernel.standard.record.scope-policy";
    private static final String READ_SCOPE = "kernel.records.read";
    private static final String WRITE_SCOPE = "kernel.records.write";

    @Override
    public String moduleId() {
        return KernelStandardRecordContract.MODULE_ID;
    }

    @Override
    public void contribute(ModuleSecurityRegistry registry) {
        registry.registerResourcePolicy(
            KernelStandardRecordContract.RESOURCE_TYPE,
            EnumSet.allOf(AuthorizationAction.class),
            this::evaluatePolicy
        );
    }

    private AuthorizationDecision evaluatePolicy(AuthorizationRequest request) {
        Set<String> scopes = extractScopes(request);
        AuthorizationAction action = request.action();

        boolean writeAction = action == AuthorizationAction.CREATE
            || action == AuthorizationAction.UPDATE
            || action == AuthorizationAction.DELETE;

        if (writeAction && scopes.contains(WRITE_SCOPE)) {
            return AuthorizationDecision.allow(POLICY_SOURCE);
        }
        if (!writeAction && (scopes.contains(READ_SCOPE) || scopes.contains(WRITE_SCOPE))) {
            return AuthorizationDecision.allow(POLICY_SOURCE);
        }

        return AuthorizationDecision.deny(DenyReason.RESOURCE_ACCESS_DENIED, POLICY_SOURCE);
    }

    private Set<String> extractScopes(AuthorizationRequest request) {
        Set<String> scopes = new LinkedHashSet<>();
        appendScopes(scopes, request.securityContext().claims().get("scope"));
        appendScopes(scopes, request.securityContext().claims().get("scp"));
        return Set.copyOf(scopes);
    }

    private void appendScopes(Set<String> scopes, String rawScopes) {
        if (rawScopes == null || rawScopes.isBlank()) {
            return;
        }
        SCOPE_SPLIT_PATTERN.splitAsStream(rawScopes)
            .filter(scope -> scope != null && !scope.isBlank())
            .map(String::trim)
            .forEach(scopes::add);
    }
}
