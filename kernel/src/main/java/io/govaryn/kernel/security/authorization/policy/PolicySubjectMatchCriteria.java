package io.govaryn.kernel.security.authorization.policy;

import java.util.List;
import java.util.Map;

public record PolicySubjectMatchCriteria(
    List<String> roles
) {
    public PolicySubjectMatchCriteria {
        roles = PolicyDocumentValidation.sanitizeStringList(roles, "subject.roles", true);
    }

    static PolicySubjectMatchCriteria fromMap(Map<String, Object> map, String fieldName) {
        List<String> roles = PolicyDocumentValidation.requireStringList(map.get("roles"), fieldName + ".roles", true);
        return new PolicySubjectMatchCriteria(roles);
    }
}
