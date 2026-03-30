package io.govaryn.kernel.security.authorization;

import io.govaryn.kernel.api.KernelAuthorizationOperation;
import io.govaryn.kernel.api.KernelAuthorizationOperations;
import io.govaryn.kernel.api.KernelAuthorizationService;
import io.govaryn.kernel.security.authorization.model.AuthorizationDecision;
import io.govaryn.kernel.security.authorization.model.AuthorizationDecisionResult;
import io.govaryn.kernel.security.authorization.model.AuthorizationRequest;
import io.govaryn.kernel.security.authorization.model.AuthorizationSubject;
import io.govaryn.kernel.security.authorization.model.DecisionReasonCode;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class KernelAuthorizationServiceAdapter implements KernelAuthorizationService {

    private final AuthorizationService authorizationService;

    public KernelAuthorizationServiceAdapter(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @Override
    public AuthorizationDecision authorize(AuthorizationSubject subject, KernelAuthorizationOperation operation) {
        if (subject == null || operation == null) {
            return denyInvalidRequest();
        }

        try {
            return authorizationService.authorize(new AuthorizationRequest(
                subject,
                operation.action(),
                operation.resourceType(),
                operation.resourceId(),
                operation.context()
            ));
        } catch (Exception ex) {
            return denyInvalidRequest();
        }
    }

    @Override
    public AuthorizationDecision authorize(
        AuthorizationSubject subject,
        String action,
        String resourceType,
        String resourceId,
        Map<String, String> context
    ) {
        return KernelAuthorizationOperations.tryCreate(action, resourceType, resourceId, context)
            .map(operation -> authorize(subject, operation))
            .orElseGet(this::denyInvalidRequest);
    }

    private AuthorizationDecision denyInvalidRequest() {
        return new AuthorizationDecision(AuthorizationDecisionResult.DENY, DecisionReasonCode.INVALID_REQUEST, null);
    }
}
