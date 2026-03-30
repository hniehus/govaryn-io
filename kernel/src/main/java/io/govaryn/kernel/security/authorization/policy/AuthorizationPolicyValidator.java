package io.govaryn.kernel.security.authorization.policy;

import io.govaryn.kernel.security.authorization.model.PolicyEffect;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class AuthorizationPolicyValidator implements AuthorizationPolicySemanticValidator {

    private static final Set<String> SUPPORTED_CONTEXT_TOP_LEVEL_KEYS = Set.of("attributes");
    private static final Set<String> SUPPORTED_CONTEXT_ATTRIBUTE_KEYS = Set.of("environment");
    private static final Set<String> SUPPORTED_CONTEXT_ATTRIBUTE_OPERATORS = Set.of("anyOf");

    @Override
    public PolicySetDocument validate(ParsedPolicySet parsedPolicy) {
        List<String> errors = new ArrayList<>();
        String policySetRevision = sanitizeRequiredString(parsedPolicy == null ? null : parsedPolicy.policySetRevision(), "policySetRevision", errors);

        List<ParsedPolicyRule> parsedRules = parsedPolicy == null ? null : parsedPolicy.rules();
        if (parsedRules == null || parsedRules.isEmpty()) {
            errors.add("rules must not be empty");
            throwIfAny(errors);
            return null;
        }

        Set<String> ruleIds = new LinkedHashSet<>();
        List<PolicyRuleDocument> validatedRules = new ArrayList<>();
        for (int i = 0; i < parsedRules.size(); i++) {
            ParsedPolicyRule parsedRule = parsedRules.get(i);
            String rulePath = "rules[" + i + "]";
            PolicyRuleDocument validatedRule = validateRule(parsedRule, rulePath, ruleIds, errors);
            if (validatedRule != null) {
                validatedRules.add(validatedRule);
            }
        }

        throwIfAny(errors);
        return new PolicySetDocument(policySetRevision, validatedRules);
    }

    private PolicyRuleDocument validateRule(
        ParsedPolicyRule parsedRule,
        String rulePath,
        Set<String> ruleIds,
        List<String> errors
    ) {
        if (parsedRule == null) {
            errors.add(rulePath + " must not be null");
            return null;
        }

        String id = sanitizeRequiredString(parsedRule.id(), rulePath + ".id", errors);
        if (id != null && !ruleIds.add(id)) {
            errors.add("rules contain duplicate id: " + id);
        }

        PolicyEffect effect = parseEffect(parsedRule.effect(), rulePath + ".effect", errors);
        PolicySubjectMatchCriteria subject = validateSubject(parsedRule.subject(), rulePath + ".subject", errors);
        List<String> actions = sanitizeRequiredList(parsedRule.actions(), rulePath + ".actions", errors);
        List<String> resourceTypes = sanitizeRequiredList(parsedRule.resourceTypes(), rulePath + ".resourceTypes", errors);
        List<String> resourceIds = sanitizeOptionalList(parsedRule.resourceIds(), rulePath + ".resourceIds", errors);
        PolicyContextMatchCriteria context = validateContext(parsedRule.context(), rulePath + ".context", errors);

        if (id == null || effect == null || subject == null || actions == null || resourceTypes == null || context == null) {
            return null;
        }
        return new PolicyRuleDocument(id, effect, subject, actions, resourceTypes, resourceIds, context);
    }

    private PolicySubjectMatchCriteria validateSubject(ParsedPolicySubject subject, String fieldName, List<String> errors) {
        if (subject == null) {
            errors.add(fieldName + " must not be null");
            return null;
        }
        List<String> roles = sanitizeRequiredList(subject.roles(), fieldName + ".roles", errors);
        if (roles == null) {
            return null;
        }
        return new PolicySubjectMatchCriteria(roles);
    }

    private PolicyContextMatchCriteria validateContext(Map<String, Object> contextRaw, String fieldName, List<String> errors) {
        if (contextRaw == null) {
            return new PolicyContextMatchCriteria(Map.of());
        }

        Map<String, PolicyContextAttributeCondition> attributes = new LinkedHashMap<>();
        for (String key : contextRaw.keySet()) {
            if (!SUPPORTED_CONTEXT_TOP_LEVEL_KEYS.contains(key)) {
                errors.add(fieldName + " contains unsupported key: " + key);
            }
        }

        Object attributesRaw = contextRaw.get("attributes");
        if (attributesRaw == null) {
            return new PolicyContextMatchCriteria(Map.of());
        }
        if (!(attributesRaw instanceof Map<?, ?> rawAttributesMap)) {
            errors.add(fieldName + ".attributes must be an object");
            return null;
        }

        for (Map.Entry<?, ?> rawAttributeEntry : rawAttributesMap.entrySet()) {
            if (!(rawAttributeEntry.getKey() instanceof String attributeKey)) {
                errors.add(fieldName + ".attributes keys must be strings");
                continue;
            }
            if (!SUPPORTED_CONTEXT_ATTRIBUTE_KEYS.contains(attributeKey)) {
                errors.add(fieldName + ".attributes contains unsupported key: " + attributeKey);
                continue;
            }

            Object conditionRaw = rawAttributeEntry.getValue();
            if (!(conditionRaw instanceof Map<?, ?> rawConditionMap)) {
                errors.add(fieldName + ".attributes." + attributeKey + " must be an object");
                continue;
            }

            for (Object operatorRaw : rawConditionMap.keySet()) {
                if (!(operatorRaw instanceof String operator)) {
                    errors.add(fieldName + ".attributes." + attributeKey + " operators must be strings");
                    continue;
                }
                if (!SUPPORTED_CONTEXT_ATTRIBUTE_OPERATORS.contains(operator)) {
                    errors.add(fieldName + ".attributes." + attributeKey + " has unsupported operator: " + operator);
                }
            }

            List<String> anyOf = null;
            Object anyOfRaw = rawConditionMap.get("anyOf");
            if (anyOfRaw == null) {
                errors.add(fieldName + ".attributes." + attributeKey + ".anyOf must not be null");
            } else if (!(anyOfRaw instanceof List<?> rawAnyOfList)) {
                errors.add(fieldName + ".attributes." + attributeKey + ".anyOf must be a list");
            } else {
                anyOf = sanitizeRequiredStringList(rawAnyOfList, fieldName + ".attributes." + attributeKey + ".anyOf", errors);
            }

            if (anyOf != null) {
                attributes.put(attributeKey, new PolicyContextAttributeCondition(anyOf));
            }
        }

        return new PolicyContextMatchCriteria(attributes);
    }

    private PolicyEffect parseEffect(String rawEffect, String fieldName, List<String> errors) {
        String normalized = sanitizeRequiredString(rawEffect, fieldName, errors);
        if (normalized == null) {
            return null;
        }
        try {
            return PolicyEffect.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            errors.add(fieldName + " must be PERMIT or DENY");
            return null;
        }
    }

    private List<String> sanitizeRequiredList(List<String> values, String fieldName, List<String> errors) {
        if (values == null) {
            errors.add(fieldName + " must not be null");
            return null;
        }
        List<String> sanitized = sanitizeRequiredStringList(values, fieldName, errors);
        if (sanitized == null || sanitized.isEmpty()) {
            errors.add(fieldName + " must not be empty");
            return null;
        }
        return sanitized;
    }

    private List<String> sanitizeOptionalList(List<String> values, String fieldName, List<String> errors) {
        if (values == null) {
            return List.of();
        }
        List<String> sanitized = sanitizeRequiredStringList(values, fieldName, errors);
        return sanitized == null ? List.of() : sanitized;
    }

    private List<String> sanitizeRequiredStringList(Iterable<?> values, String fieldName, List<String> errors) {
        List<String> sanitized = new ArrayList<>();
        for (Object valueRaw : values) {
            if (!(valueRaw instanceof String value)) {
                errors.add(fieldName + " items must be strings");
                continue;
            }
            String normalized = sanitizeRequiredString(value, fieldName + "[]", errors);
            if (normalized != null) {
                sanitized.add(normalized);
            }
        }
        return List.copyOf(sanitized);
    }

    private String sanitizeRequiredString(String value, String fieldName, List<String> errors) {
        if (value == null || value.isBlank()) {
            errors.add(fieldName + " must not be blank");
            return null;
        }
        return value.trim();
    }

    private void throwIfAny(List<String> errors) {
        if (!errors.isEmpty()) {
            throw new PolicyValidationException(errors);
        }
    }
}
