package io.govaryn.kernel.security;

import java.util.List;

public record KernelSecurityIdentity(
    String subject,
    String issuer,
    String username,
    List<String> authorities
) {
}
