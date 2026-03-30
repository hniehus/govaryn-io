package io.govaryn.kernel.security.authorization.policy;

import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class YamlAuthorizationPolicyParser {

    public ParsedPolicySet parse(Path policyPath) {
        return parse(new FileSystemResource(policyPath));
    }

    public ParsedPolicySet parse(Resource resource) {
        Map<String, Object> root = loadYamlAsMap(resource);
        return toParsedPolicySet(root, resource.getDescription());
    }

    private Map<String, Object> loadYamlAsMap(Resource resource) {
        try {
            YamlMapFactoryBean yaml = new YamlMapFactoryBean();
            yaml.setResources(resource);
            Map<String, Object> root = yaml.getObject();
            if (root == null) {
                throw new PolicyParseException("Policy YAML is empty: " + resource.getDescription());
            }
            return root;
        } catch (PolicyParseException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new PolicyParseException("Failed to parse policy YAML: " + resource.getDescription(), ex);
        }
    }

    private ParsedPolicySet toParsedPolicySet(Map<String, Object> root, String sourceDescription) {
        String revision = asNullableString(root.get("policySetRevision"), "policySetRevision", sourceDescription);
        List<ParsedPolicyRule> rules = asRuleList(root.get("rules"), "rules", sourceDescription);
        return new ParsedPolicySet(revision, rules);
    }

    private List<ParsedPolicyRule> asRuleList(Object value, String fieldName, String sourceDescription) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof List<?> list)) {
            throw parseTypeError(fieldName, "list", sourceDescription);
        }
        List<ParsedPolicyRule> rules = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            if (!(item instanceof Map<?, ?> rawRule)) {
                throw parseTypeError(fieldName + "[" + i + "]", "object", sourceDescription);
            }
            Map<String, Object> rule = toStringKeyedMap(rawRule);
            rules.add(
                new ParsedPolicyRule(
                    asNullableString(rule.get("id"), fieldName + "[" + i + "].id", sourceDescription),
                    asNullableString(rule.get("effect"), fieldName + "[" + i + "].effect", sourceDescription),
                    asSubject(rule.get("subject"), fieldName + "[" + i + "].subject", sourceDescription),
                    asNullableStringList(rule.get("actions"), fieldName + "[" + i + "].actions", sourceDescription),
                    asNullableStringList(rule.get("resourceTypes"), fieldName + "[" + i + "].resourceTypes", sourceDescription),
                    asNullableStringList(rule.get("resourceIds"), fieldName + "[" + i + "].resourceIds", sourceDescription),
                    asNullableObjectMap(rule.get("context"), fieldName + "[" + i + "].context", sourceDescription)
                )
            );
        }
        return List.copyOf(rules);
    }

    private ParsedPolicySubject asSubject(Object value, String fieldName, String sourceDescription) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof Map<?, ?> rawSubject)) {
            throw parseTypeError(fieldName, "object", sourceDescription);
        }
        Map<String, Object> subject = toStringKeyedMap(rawSubject);
        return new ParsedPolicySubject(
            asNullableStringList(subject.get("roles"), fieldName + ".roles", sourceDescription)
        );
    }

    private String asNullableString(Object value, String fieldName, String sourceDescription) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof String text)) {
            throw parseTypeError(fieldName, "string", sourceDescription);
        }
        return text;
    }

    private List<String> asNullableStringList(Object value, String fieldName, String sourceDescription) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof List<?> list)) {
            throw parseTypeError(fieldName, "list", sourceDescription);
        }
        List<String> values = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            Object entry = list.get(i);
            if (!(entry instanceof String text)) {
                throw parseTypeError(fieldName + "[" + i + "]", "string", sourceDescription);
            }
            values.add(text);
        }
        return List.copyOf(values);
    }

    private Map<String, Object> asNullableObjectMap(Object value, String fieldName, String sourceDescription) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof Map<?, ?> rawMap)) {
            throw parseTypeError(fieldName, "object", sourceDescription);
        }
        return Map.copyOf(toStringKeyedMap(rawMap));
    }

    private Map<String, Object> toStringKeyedMap(Map<?, ?> rawMap) {
        Map<String, Object> mapped = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw new PolicyParseException("Policy YAML object keys must be strings");
            }
            mapped.put(key, entry.getValue());
        }
        return mapped;
    }

    private PolicyParseException parseTypeError(String fieldName, String expectedType, String sourceDescription) {
        return new PolicyParseException(
            "Invalid YAML structure in " + sourceDescription + ": field '" + fieldName + "' must be a " + expectedType
        );
    }
}
