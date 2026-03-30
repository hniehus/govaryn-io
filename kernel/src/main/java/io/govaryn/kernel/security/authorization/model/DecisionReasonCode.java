package io.govaryn.kernel.security.authorization.model;

public enum DecisionReasonCode {
    PERMIT_RULE_MATCHED,
    DENY_RULE_MATCHED,
    POLICY_RULE_MATCHED,
    NO_MATCHING_RULE,
    INVALID_REQUEST,
    SUBJECT_NOT_AUTHENTICATED,
    POLICY_UNAVAILABLE,
    EVALUATION_ERROR
}
