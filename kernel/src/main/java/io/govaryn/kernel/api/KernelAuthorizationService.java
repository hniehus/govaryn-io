package io.govaryn.kernel.api;

import io.govaryn.kernel.security.authorization.model.AuthorizationDecision;
import io.govaryn.kernel.security.authorization.model.AuthorizationSubject;

import java.util.Map;

public interface KernelAuthorizationService {

    AuthorizationDecision authorize(AuthorizationSubject subject, KernelAuthorizationOperation operation);

    AuthorizationDecision authorize(
        AuthorizationSubject subject,
        String action,
        String resourceType,
        String resourceId,
        Map<String, String> context
    );
}
