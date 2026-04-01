package io.govaryn.kernel.security.authorization.framework;

import io.govaryn.kernel.security.authorization.framework.model.AuthorizationDecision;
import io.govaryn.kernel.security.authorization.framework.model.AuthorizationRequest;

/**
 * Module-supplied evaluator invoked by kernel-controlled authorization enforcement.
 */
@FunctionalInterface
public interface ResourcePolicyEvaluator {

    AuthorizationDecision evaluate(AuthorizationRequest request);
}
