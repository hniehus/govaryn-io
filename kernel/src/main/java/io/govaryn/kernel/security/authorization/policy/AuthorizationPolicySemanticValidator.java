package io.govaryn.kernel.security.authorization.policy;

public interface AuthorizationPolicySemanticValidator {

    PolicySetDocument validate(ParsedPolicySet parsedPolicy);
}
