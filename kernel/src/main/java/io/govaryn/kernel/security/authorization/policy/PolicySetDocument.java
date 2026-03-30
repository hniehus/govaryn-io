package io.govaryn.kernel.security.authorization.policy;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record PolicySetDocument(
    String policySetRevision,
    List<PolicyRuleDocument> rules
) {
    public PolicySetDocument {
        policySetRevision = PolicyDocumentValidation.requireNonBlank(policySetRevision, "policySetRevision");
        rules = rules == null ? List.of() : List.copyOf(rules);
        if (rules.isEmpty()) {
            throw new IllegalArgumentException("rules must not be empty");
        }

        Set<String> ruleIds = new LinkedHashSet<>();
        for (PolicyRuleDocument rule : rules) {
            PolicyDocumentValidation.requireNonNull(rule, "rules[]");
            if (!ruleIds.add(rule.id())) {
                throw new IllegalArgumentException("rules contain duplicate id: " + rule.id());
            }
        }
    }

    public static PolicySetDocument fromMap(Map<String, Object> root) {
        Map<String, Object> map = PolicyDocumentValidation.requireObjectMap(root, "policy document");
        String revision = PolicyDocumentValidation.requireString(map.get("policySetRevision"), "policySetRevision");

        Object rulesRaw = map.get("rules");
        if (!(rulesRaw instanceof List<?> rulesList)) {
            throw new IllegalArgumentException("rules must be a list");
        }

        List<PolicyRuleDocument> rules = new ArrayList<>();
        for (int i = 0; i < rulesList.size(); i++) {
            Object rawRule = rulesList.get(i);
            Map<String, Object> ruleMap = PolicyDocumentValidation.requireObjectMap(rawRule, "rules[" + i + "]");
            rules.add(PolicyRuleDocument.fromMap(ruleMap, "rules[" + i + "]"));
        }

        return new PolicySetDocument(revision, rules);
    }
}
