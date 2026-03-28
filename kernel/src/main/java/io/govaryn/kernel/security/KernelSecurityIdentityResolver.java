package io.govaryn.kernel.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KernelSecurityIdentityResolver {

    public KernelSecurityIdentity resolve(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)) {
            throw new IllegalArgumentException("Authenticated JWT principal is required");
        }

        String issuer = jwtAuthenticationToken.getToken().getIssuer() != null
            ? jwtAuthenticationToken.getToken().getIssuer().toString()
            : null;

        List<String> authorities = jwtAuthenticationToken.getAuthorities().stream()
            .map(grantedAuthority -> grantedAuthority.getAuthority())
            .toList();

        return new KernelSecurityIdentity(
            jwtAuthenticationToken.getToken().getSubject(),
            issuer,
            jwtAuthenticationToken.getName(),
            authorities
        );
    }
}
