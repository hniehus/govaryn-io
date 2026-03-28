package io.govaryn.kernel.config;

import jakarta.validation.constraints.AssertTrue;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.util.List;

@ConfigurationProperties(prefix = "govaryn.kernel.security")
@Validated
public class GovarynKernelSecurityProperties {

    private boolean enabled = false;
    private String issuerUri;
    private String audience;
    private List<String> publicPaths = List.of();
    private String authorityClaim = "scope";
    private String authorityPrefix = "SCOPE_";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getIssuerUri() {
        return issuerUri;
    }

    public void setIssuerUri(String issuerUri) {
        this.issuerUri = issuerUri;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public List<String> getPublicPaths() {
        return publicPaths;
    }

    public void setPublicPaths(List<String> publicPaths) {
        this.publicPaths = publicPaths == null ? List.of() : List.copyOf(publicPaths);
    }

    public String getAuthorityClaim() {
        return authorityClaim;
    }

    public void setAuthorityClaim(String authorityClaim) {
        this.authorityClaim = authorityClaim;
    }

    public String getAuthorityPrefix() {
        return authorityPrefix;
    }

    public void setAuthorityPrefix(String authorityPrefix) {
        this.authorityPrefix = authorityPrefix;
    }

    @AssertTrue(message = "govaryn.kernel.security.issuer-uri must be configured when govaryn.kernel.security.enabled=true")
    boolean isIssuerUriPresentWhenEnabled() {
        return !enabled || hasText(issuerUri);
    }

    @AssertTrue(message = "govaryn.kernel.security.issuer-uri must be a valid absolute URI when govaryn.kernel.security.enabled=true")
    boolean isIssuerUriValidWhenEnabled() {
        if (!enabled || !hasText(issuerUri)) {
            return true;
        }

        try {
            URI uri = URI.create(issuerUri);
            return uri.isAbsolute() && uri.getHost() != null;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    @AssertTrue(message = "govaryn.kernel.security.audience must be configured when govaryn.kernel.security.enabled=true")
    boolean isAudiencePresentWhenEnabled() {
        return !enabled || hasText(audience);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
