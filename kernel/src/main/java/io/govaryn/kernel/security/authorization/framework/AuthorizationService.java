package io.govaryn.kernel.security.authorization.framework;

import io.govaryn.kernel.security.authorization.framework.model.AuthorizationDecision;
import io.govaryn.kernel.security.authorization.framework.model.AuthorizationRequest;

/**
 * Kernel-controlled authorization entry point for standard backend paths.
 */
public interface AuthorizationService {

    AuthorizationDecision authorize(AuthorizationRequest request);
}
