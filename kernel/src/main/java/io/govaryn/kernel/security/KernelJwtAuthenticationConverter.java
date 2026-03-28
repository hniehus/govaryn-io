package io.govaryn.kernel.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class KernelJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final KernelJwtAuthoritiesConverter authoritiesConverter;

    public KernelJwtAuthenticationConverter(KernelJwtAuthoritiesConverter authoritiesConverter) {
        this.authoritiesConverter = authoritiesConverter;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        return new JwtAuthenticationToken(jwt, authoritiesConverter.convert(jwt), resolveUsername(jwt));
    }

    private String resolveUsername(Jwt jwt) {
        String preferredUsername = jwt.getClaimAsString("preferred_username");
        if (hasText(preferredUsername)) {
            return preferredUsername;
        }

        String username = jwt.getClaimAsString("username");
        if (hasText(username)) {
            return username;
        }

        return jwt.getSubject();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
