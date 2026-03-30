package io.govaryn.kernel.security.authorization;

import io.govaryn.kernel.security.authorization.model.AuthorizationDecision;
import io.govaryn.kernel.security.authorization.model.AuthorizationDecisionResult;
import io.govaryn.kernel.security.authorization.model.AuthorizationRequest;
import io.govaryn.kernel.security.authorization.model.DecisionReasonCode;
import io.govaryn.kernel.security.authorization.model.PolicyEffect;
import io.govaryn.kernel.security.authorization.policy.ActiveAuthorizationPolicySnapshot;
import io.govaryn.kernel.security.authorization.policy.ActiveAuthorizationPolicyStore;
import io.govaryn.kernel.security.authorization.policy.PolicyContextAttributeCondition;
import io.govaryn.kernel.security.authorization.policy.PolicyRuleDocument;
import io.govaryn.kernel.security.authorization.policy.PolicySetDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class KernelPolicyDecisionPoint implements AuthorizationService {

    private static final Logger log = LoggerFactory.getLogger(KernelPolicyDecisionPoint.class);

    private final ActiveAuthorizationPolicyStore activePolicyStore;
    private final AuthorizationDecisionLogger decisionLogger;

    public KernelPolicyDecisionPoint(
        ActiveAuthorizationPolicyStore activePolicyStore,
        AuthorizationDecisionLogger decisionLogger
    ) {
        this.activePolicyStore = activePolicyStore;
        this.decisionLogger = decisionLogger;
    }

    @Override
    public AuthorizationDecision authorize(AuthorizationRequest request) {
        if (request == null) {
            AuthorizationDecision decision = new AuthorizationDecision(
                AuthorizationDecisionResult.DENY,
                DecisionReasonCode.INVALID_REQUEST,
                null
            );
            decisionLogger.logDecision(null, decision, "none", null);
            return decision;
        }

        String activeRevision = "none";
        try {
            Optional<ActiveAuthorizationPolicySnapshot> active = activePolicyStore.getActivePolicy();
            if (active.isEmpty()) {
                AuthorizationDecision decision = new AuthorizationDecision(
                    AuthorizationDecisionResult.DENY,
                    DecisionReasonCode.POLICY_UNAVAILABLE,
                    null
                );
                decisionLogger.logDecision(request, decision, activeRevision, null);
                return decision;
            }

            ActiveAuthorizationPolicySnapshot snapshot = active.get();
            PolicySetDocument policy = snapshot.policySet();
            activeRevision = snapshot.revision();
            String matchingPermitRuleId = null;
            String matchingDenyRuleId = null;

            for (PolicyRuleDocument rule : policy.rules()) {
                if (!matches(rule, request)) {
                    continue;
                }
                if (rule.effect() == PolicyEffect.DENY && matchingDenyRuleId == null) {
                    matchingDenyRuleId = rule.id();
                } else if (rule.effect() == PolicyEffect.PERMIT && matchingPermitRuleId == null) {
                    matchingPermitRuleId = rule.id();
                }
            }

            if (matchingDenyRuleId != null) {
                AuthorizationDecision decision = new AuthorizationDecision(
                    AuthorizationDecisionResult.DENY,
                    DecisionReasonCode.DENY_RULE_MATCHED,
                    matchingDenyRuleId
                );
                decisionLogger.logDecision(request, decision, activeRevision, null);
                return decision;
            }
            if (matchingPermitRuleId != null) {
                AuthorizationDecision decision = new AuthorizationDecision(
                    AuthorizationDecisionResult.PERMIT,
                    DecisionReasonCode.PERMIT_RULE_MATCHED,
                    matchingPermitRuleId
                );
                decisionLogger.logDecision(request, decision, activeRevision, null);
                return decision;
            }
            AuthorizationDecision decision = new AuthorizationDecision(
                AuthorizationDecisionResult.DENY,
                DecisionReasonCode.NO_MATCHING_RULE,
                null
            );
            decisionLogger.logDecision(request, decision, activeRevision, null);
            return decision;
        } catch (Exception ex) {
            log.warn("event=authorization_decision_failed reason=evaluation_error errorType={}", ex.getClass().getSimpleName());
            AuthorizationDecision decision = new AuthorizationDecision(
                AuthorizationDecisionResult.DENY,
                DecisionReasonCode.EVALUATION_ERROR,
                null
            );
            decisionLogger.logDecision(request, decision, activeRevision, ex);
            return decision;
        }
    }

    private boolean matches(PolicyRuleDocument rule, AuthorizationRequest request) {
        return matchesSubject(rule, request)
            && matchesAction(rule, request)
            && matchesResourceType(rule, request)
            && matchesResourceId(rule, request)
            && matchesContext(rule, request);
    }

    private boolean matchesSubject(PolicyRuleDocument rule, AuthorizationRequest request) {
        List<String> roles = request.subject().roles();
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        return roles.stream().anyMatch(role -> rule.subject().roles().contains(role));
    }

    private boolean matchesAction(PolicyRuleDocument rule, AuthorizationRequest request) {
        return rule.actions().contains(request.action());
    }

    private boolean matchesResourceType(PolicyRuleDocument rule, AuthorizationRequest request) {
        return rule.resourceTypes().contains(request.resourceType());
    }

    private boolean matchesResourceId(PolicyRuleDocument rule, AuthorizationRequest request) {
        if (rule.resourceIds().isEmpty()) {
            return true;
        }
        return request.resourceId() != null && rule.resourceIds().contains(request.resourceId());
    }

    private boolean matchesContext(PolicyRuleDocument rule, AuthorizationRequest request) {
        Map<String, PolicyContextAttributeCondition> attributes = rule.context().attributes();
        if (attributes.isEmpty()) {
            return true;
        }
        Map<String, String> requestContext = request.context();
        if (requestContext == null || requestContext.isEmpty()) {
            return false;
        }
        for (Map.Entry<String, PolicyContextAttributeCondition> conditionEntry : attributes.entrySet()) {
            String actualValue = requestContext.get(conditionEntry.getKey());
            if (actualValue == null || actualValue.isBlank()) {
                return false;
            }
            if (!conditionEntry.getValue().anyOf().contains(actualValue)) {
                return false;
            }
        }
        return true;
    }
}
