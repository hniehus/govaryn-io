package io.govaryn.kernel.security.authorization.model;

public enum DecisionReasonCode {
    POLICY_RULE_MATCHED,
    NO_MATCHING_RULE,
    INVALID_REQUEST,
    SUBJECT_NOT_AUTHENTICATED,
    POLICY_UNAVAILABLE
}
