package io.govaryn.kernel.security.authorization.model;

import java.util.List;

public record PolicySet(
    String policySetId,
    List<PolicyRule> rules
) {
    public PolicySet {
        policySetId = AuthorizationModelValidation.requireNonBlank(policySetId, "policySetId");
        if (rules == null) {
            rules = List.of();
        } else {
            for (PolicyRule rule : rules) {
                AuthorizationModelValidation.requireNonNull(rule, "rules[]");
            }
            rules = List.copyOf(rules);
        }
    }
}
