package io.govaryn.kernel.security.authorization.policy;

import io.govaryn.kernel.security.authorization.model.PolicyEffect;

import java.util.List;
import java.util.Map;
import java.util.Locale;

public record PolicyRuleDocument(
    String id,
    PolicyEffect effect,
    PolicySubjectMatchCriteria subject,
    List<String> actions,
    List<String> resourceTypes,
    List<String> resourceIds,
    PolicyContextMatchCriteria context
) {
    public PolicyRuleDocument {
        id = PolicyDocumentValidation.requireNonBlank(id, "rules[].id");
        effect = PolicyDocumentValidation.requireNonNull(effect, "rules[].effect");
        subject = PolicyDocumentValidation.requireNonNull(subject, "rules[].subject");
        actions = PolicyDocumentValidation.sanitizeStringList(actions, "rules[].actions", true);
        resourceTypes = PolicyDocumentValidation.sanitizeStringList(resourceTypes, "rules[].resourceTypes", true);
        resourceIds = PolicyDocumentValidation.sanitizeStringList(resourceIds, "rules[].resourceIds", false);
        context = context == null ? new PolicyContextMatchCriteria(Map.of()) : context;
    }

    static PolicyRuleDocument fromMap(Map<String, Object> map, String fieldName) {
        String id = PolicyDocumentValidation.requireString(map.get("id"), fieldName + ".id");
        String effectRaw = PolicyDocumentValidation.requireString(map.get("effect"), fieldName + ".effect");

        PolicyEffect effect;
        try {
            effect = PolicyEffect.valueOf(effectRaw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(fieldName + ".effect must be PERMIT or DENY");
        }

        Map<String, Object> subjectMap = PolicyDocumentValidation.requireObjectMap(map.get("subject"), fieldName + ".subject");
        PolicySubjectMatchCriteria subject = PolicySubjectMatchCriteria.fromMap(subjectMap, fieldName + ".subject");

        List<String> actions = PolicyDocumentValidation.requireStringList(map.get("actions"), fieldName + ".actions", true);
        List<String> resourceTypes = PolicyDocumentValidation.requireStringList(map.get("resourceTypes"), fieldName + ".resourceTypes", true);
        List<String> resourceIds = PolicyDocumentValidation.requireStringList(map.get("resourceIds"), fieldName + ".resourceIds", false);

        PolicyContextMatchCriteria context = map.containsKey("context")
            ? PolicyContextMatchCriteria.fromMap(
                PolicyDocumentValidation.requireObjectMap(map.get("context"), fieldName + ".context"),
                fieldName + ".context"
            )
            : new PolicyContextMatchCriteria(Map.of());

        return new PolicyRuleDocument(id, effect, subject, actions, resourceTypes, resourceIds, context);
    }
}
