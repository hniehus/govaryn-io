package io.govaryn.kernel.security.authorization.model;

public record AuthorizationDecision(
    AuthorizationDecisionResult result,
    DecisionReasonCode reasonCode,
    String matchedRuleId
) {
    public AuthorizationDecision {
        result = AuthorizationModelValidation.requireNonNull(result, "result");
        reasonCode = AuthorizationModelValidation.requireNonNull(reasonCode, "reasonCode");
        matchedRuleId = matchedRuleId == null ? null : AuthorizationModelValidation.requireNonBlank(matchedRuleId, "matchedRuleId");
    }
}
