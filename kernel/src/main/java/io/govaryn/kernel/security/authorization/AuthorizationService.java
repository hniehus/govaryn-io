package io.govaryn.kernel.security.authorization;

import io.govaryn.kernel.security.authorization.model.AuthorizationDecision;
import io.govaryn.kernel.security.authorization.model.AuthorizationRequest;

public interface AuthorizationService {

    AuthorizationDecision authorize(AuthorizationRequest request);
}
