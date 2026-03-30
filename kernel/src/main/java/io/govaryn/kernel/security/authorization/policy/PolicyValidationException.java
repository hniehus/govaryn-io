package io.govaryn.kernel.security.authorization.policy;

import java.util.List;

public class PolicyValidationException extends RuntimeException {

    private final List<String> errors;

    public PolicyValidationException(List<String> errors) {
        super(formatMessage(errors));
        this.errors = List.copyOf(errors);
    }

    public List<String> errors() {
        return errors;
    }

    private static String formatMessage(List<String> errors) {
        if (errors == null || errors.isEmpty()) {
            return "Policy validation failed";
        }
        return "Policy validation failed with " + errors.size() + " error(s): " + String.join("; ", errors);
    }
}
