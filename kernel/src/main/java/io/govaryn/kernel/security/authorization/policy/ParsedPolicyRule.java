package io.govaryn.kernel.security.authorization.policy;

import java.util.List;
import java.util.Map;

public record ParsedPolicyRule(
    String id,
    String effect,
    ParsedPolicySubject subject,
    List<String> actions,
    List<String> resourceTypes,
    List<String> resourceIds,
    Map<String, Object> context
) {
    public ParsedPolicyRule {
        actions = actions == null ? null : List.copyOf(actions);
        resourceTypes = resourceTypes == null ? null : List.copyOf(resourceTypes);
        resourceIds = resourceIds == null ? null : List.copyOf(resourceIds);
        context = context == null ? null : Map.copyOf(context);
    }
}
