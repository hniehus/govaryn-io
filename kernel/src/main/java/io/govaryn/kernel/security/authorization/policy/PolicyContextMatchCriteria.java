package io.govaryn.kernel.security.authorization.policy;

import java.util.LinkedHashMap;
import java.util.Map;

public record PolicyContextMatchCriteria(
    Map<String, PolicyContextAttributeCondition> attributes
) {
    public PolicyContextMatchCriteria {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        for (Map.Entry<String, PolicyContextAttributeCondition> entry : attributes.entrySet()) {
            PolicyDocumentValidation.requireNonBlank(entry.getKey(), "context attribute key");
            PolicyDocumentValidation.requireNonNull(entry.getValue(), "context attribute condition");
        }
    }

    static PolicyContextMatchCriteria fromMap(Map<String, Object> map, String fieldName) {
        Object attributesRaw = map.get("attributes");
        if (attributesRaw == null) {
            return new PolicyContextMatchCriteria(Map.of());
        }
        Map<String, Object> attributesMap = PolicyDocumentValidation.requireObjectMap(attributesRaw, fieldName + ".attributes");
        Map<String, PolicyContextAttributeCondition> attributes = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : attributesMap.entrySet()) {
            Map<String, Object> conditionMap = PolicyDocumentValidation.requireObjectMap(
                entry.getValue(),
                fieldName + ".attributes." + entry.getKey()
            );
            attributes.put(
                entry.getKey(),
                PolicyContextAttributeCondition.fromMap(conditionMap, fieldName + ".attributes." + entry.getKey())
            );
        }
        return new PolicyContextMatchCriteria(attributes);
    }
}
