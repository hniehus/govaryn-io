package io.govaryn.kernel.security.authorization.policy;

import java.util.List;

public record ParsedPolicySubject(
    List<String> roles
) {
    public ParsedPolicySubject {
        roles = roles == null ? null : List.copyOf(roles);
    }
}
