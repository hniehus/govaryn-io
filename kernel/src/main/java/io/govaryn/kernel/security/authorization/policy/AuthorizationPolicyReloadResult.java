package io.govaryn.kernel.security.authorization.policy;

public record AuthorizationPolicyReloadResult(
    boolean success,
    String revision,
    String errorMessage
) {
    public static AuthorizationPolicyReloadResult success(String revision) {
        return new AuthorizationPolicyReloadResult(true, revision, null);
    }

    public static AuthorizationPolicyReloadResult failure(String errorMessage) {
        return new AuthorizationPolicyReloadResult(false, null, errorMessage);
    }
}
