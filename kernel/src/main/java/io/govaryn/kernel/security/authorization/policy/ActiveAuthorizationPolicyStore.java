package io.govaryn.kernel.security.authorization.policy;

import java.util.Optional;

public interface ActiveAuthorizationPolicyStore {

    Optional<ActiveAuthorizationPolicySnapshot> getActivePolicy();

    ActiveAuthorizationPolicySnapshot activate(PolicySetDocument policySet);
}
