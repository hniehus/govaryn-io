package io.govaryn.kernel.security.authorization.framework.model;

/**
 * Kernel authorization outcome.
 */
public record AuthorizationDecision(
    boolean allowed,
    DenyReason denyReason,
    String policySource
) {
    public AuthorizationDecision {
        policySource = AuthorizationFrameworkModelValidation.trimToNull(policySource);
        if (allowed && denyReason != null) {
            throw new IllegalArgumentException("denyReason must be null when decision is allowed");
        }
        if (!allowed && denyReason == null) {
            throw new IllegalArgumentException("denyReason must be provided when decision is denied");
        }
    }

    public static AuthorizationDecision allow(String policySource) {
        return new AuthorizationDecision(true, null, policySource);
    }

    public static AuthorizationDecision deny(DenyReason denyReason, String policySource) {
        return new AuthorizationDecision(false, denyReason, policySource);
    }
}
