package io.govaryn.kernel.security.authorization.policy;

import java.time.Instant;

public record ActiveAuthorizationPolicySnapshot(
    String revision,
    PolicySetDocument policySet,
    Instant activatedAt
) {
    public ActiveAuthorizationPolicySnapshot {
        if (revision == null || revision.isBlank()) {
            throw new IllegalArgumentException("revision must not be blank");
        }
        if (policySet == null) {
            throw new IllegalArgumentException("policySet must not be null");
        }
        if (activatedAt == null) {
            throw new IllegalArgumentException("activatedAt must not be null");
        }
    }
}
