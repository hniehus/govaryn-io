package io.govaryn.kernel.security.authorization.policy;

import java.util.List;

public record ParsedPolicySet(
    String policySetRevision,
    List<ParsedPolicyRule> rules
) {
    public ParsedPolicySet {
        rules = rules == null ? null : List.copyOf(rules);
    }
}
