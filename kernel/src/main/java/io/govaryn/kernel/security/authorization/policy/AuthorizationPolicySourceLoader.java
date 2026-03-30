package io.govaryn.kernel.security.authorization.policy;

import java.nio.file.Path;

public interface AuthorizationPolicySourceLoader {

    ParsedPolicySet load(Path policyPath);
}
