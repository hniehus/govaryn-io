package io.govaryn.kernel.security.authorization.policy;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class InMemoryActiveAuthorizationPolicyStore implements ActiveAuthorizationPolicyStore {

    private final AtomicReference<ActiveAuthorizationPolicySnapshot> active = new AtomicReference<>();

    @Override
    public Optional<ActiveAuthorizationPolicySnapshot> getActivePolicy() {
        return Optional.ofNullable(active.get());
    }

    @Override
    public ActiveAuthorizationPolicySnapshot activate(PolicySetDocument policySet) {
        if (policySet == null) {
            throw new IllegalArgumentException("policySet must not be null");
        }
        ActiveAuthorizationPolicySnapshot snapshot = new ActiveAuthorizationPolicySnapshot(
            policySet.policySetRevision(),
            policySet,
            Instant.now()
        );
        active.set(snapshot);
        return snapshot;
    }
}
