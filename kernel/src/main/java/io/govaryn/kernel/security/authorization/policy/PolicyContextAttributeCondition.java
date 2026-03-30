package io.govaryn.kernel.security.authorization.policy;

import java.util.List;
import java.util.Map;

public record PolicyContextAttributeCondition(
    List<String> anyOf
) {
    public PolicyContextAttributeCondition {
        anyOf = PolicyDocumentValidation.sanitizeStringList(anyOf, "context.attributes[].anyOf", true);
    }

    static PolicyContextAttributeCondition fromMap(Map<String, Object> map, String fieldName) {
        Object anyOfRaw = map.get("anyOf");
        List<String> anyOfValues = PolicyDocumentValidation.requireStringList(anyOfRaw, fieldName + ".anyOf", true);
        return new PolicyContextAttributeCondition(anyOfValues);
    }
}
