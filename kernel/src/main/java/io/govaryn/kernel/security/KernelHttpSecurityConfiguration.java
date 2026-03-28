package io.govaryn.kernel.security;

import io.govaryn.kernel.config.GovarynKernelSecurityProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

import java.util.List;

@Configuration
public class KernelHttpSecurityConfiguration {

    @Bean
    SecurityFilterChain kernelSecurityFilterChain(
        HttpSecurity http,
        GovarynKernelSecurityProperties securityProperties
    ) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable);
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        if (!securityProperties.isEnabled()) {
            http.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
            return http.build();
        }

        List<String> publicPaths = securityProperties.getPublicPaths();
        http.authorizeHttpRequests(authorize -> {
            if (!publicPaths.isEmpty()) {
                authorize.requestMatchers(publicPaths.toArray(String[]::new)).permitAll();
            }
            authorize.anyRequest().authenticated();
        });

        http.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        http.exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));

        return http.build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "govaryn.kernel.security", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean(JwtDecoder.class)
    JwtDecoder kernelJwtDecoder(GovarynKernelSecurityProperties securityProperties) {
        NimbusJwtDecoder jwtDecoder = (NimbusJwtDecoder) JwtDecoders.fromIssuerLocation(securityProperties.getIssuerUri());

        OAuth2TokenValidator<Jwt> withIssuerAndTimestamps = JwtValidators.createDefaultWithIssuer(securityProperties.getIssuerUri());
        OAuth2TokenValidator<Jwt> withAudience = token -> token.getAudience().contains(securityProperties.getAudience())
            ? OAuth2TokenValidatorResult.success()
            : OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", "The required audience is missing", null)
            );

        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuerAndTimestamps, withAudience));
        return jwtDecoder;
    }
}
