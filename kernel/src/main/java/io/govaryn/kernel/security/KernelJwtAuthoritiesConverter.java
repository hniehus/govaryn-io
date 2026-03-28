package io.govaryn.kernel.security;

import io.govaryn.kernel.config.GovarynKernelSecurityProperties;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

@Component
public class KernelJwtAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final Pattern AUTHORITIES_SPLIT_PATTERN = Pattern.compile("[,\\s]+");

    private final GovarynKernelSecurityProperties securityProperties;

    public KernelJwtAuthoritiesConverter(GovarynKernelSecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Object claimValue = jwt.getClaims().get(securityProperties.getAuthorityClaim());
        if (claimValue == null) {
            return List.of();
        }

        List<String> rawAuthorities = switch (claimValue) {
            case String claimString -> AUTHORITIES_SPLIT_PATTERN.splitAsStream(claimString)
                .filter(value -> !value.isBlank())
                .toList();
            case Collection<?> claimCollection -> claimCollection.stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .filter(value -> !value.isBlank())
                .toList();
            default -> List.of();
        };

        String prefix = securityProperties.getAuthorityPrefix() == null ? "" : securityProperties.getAuthorityPrefix();
        return rawAuthorities.stream()
            .map(authority -> new SimpleGrantedAuthority(prefix + authority))
            .map(GrantedAuthority.class::cast)
            .toList();
    }
}
