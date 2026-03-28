package io.govaryn.kernel.security;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class KernelHttpSecurityConfigurationTest {

    private static final TestOidcServer OIDC = new TestOidcServer();

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        OIDC.startIfNeeded();
        registry.add("govaryn.kernel.id", () -> "test-kernel");
        registry.add("govaryn.kernel.environment", () -> "dev");
        registry.add("spring.application.version", () -> "1.2.0");
        registry.add("govaryn.kernel.security.enabled", () -> "true");
        registry.add("govaryn.kernel.security.issuer-uri", OIDC::issuerUri);
        registry.add("govaryn.kernel.security.audience", () -> "govaryn-kernel");
        registry.add("govaryn.kernel.security.authority-claim", () -> "roles");
        registry.add("govaryn.kernel.security.authority-prefix", () -> "ROLE_");
        registry.add("govaryn.kernel.security.public-paths[0]", () -> "/health");
    }

    @AfterAll
    static void shutdownOidcServer() {
        OIDC.stopIfRunning();
    }

    @Test
    void configuredPublicEndpointIsReachableWithoutToken() throws Exception {
        mockMvc.perform(get("/health"))
            .andExpect(status().isOk())
            .andExpect(content().json("{\"status\":\"UP\"}"));
    }

    @Test
    void protectedEndpointWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/kernel/whoami"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void validJwtFromConfiguredIssuerAndAudienceAuthenticates() throws Exception {
        String token = OIDC.issueToken(
            "kernel-user",
            OIDC.issuerUri(),
            "govaryn-kernel",
            Instant.now(),
            Instant.now().minusSeconds(10),
            Instant.now().plusSeconds(300),
            Map.of(
                "preferred_username", "alice",
                "roles", java.util.List.of("admin", "support")
            )
        );

        mockMvc.perform(get("/api/kernel/whoami").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.subject").value("kernel-user"))
            .andExpect(jsonPath("$.issuer").value(OIDC.issuerUri()))
            .andExpect(jsonPath("$.username").value("alice"))
            .andExpect(jsonPath("$.authorities[0]").value("ROLE_admin"))
            .andExpect(jsonPath("$.authorities[1]").value("ROLE_support"));
    }

    @Test
    void tokenWithWrongIssuerIsRejected() throws Exception {
        String token = OIDC.issueToken("kernel-user", OIDC.issuerUri() + "/other", "govaryn-kernel", Instant.now(), Instant.now().minusSeconds(10), Instant.now().plusSeconds(300));

        mockMvc.perform(get("/api/kernel/whoami").header("Authorization", "Bearer " + token))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void usernameFallsBackToSubjectWhenPreferredUsernameIsMissing() throws Exception {
        String token = OIDC.issueToken(
            "fallback-subject",
            OIDC.issuerUri(),
            "govaryn-kernel",
            Instant.now(),
            Instant.now().minusSeconds(10),
            Instant.now().plusSeconds(300),
            Map.of("roles", java.util.List.of("reader"))
        );

        mockMvc.perform(get("/api/kernel/whoami").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.subject").value("fallback-subject"))
            .andExpect(jsonPath("$.username").value("fallback-subject"))
            .andExpect(jsonPath("$.authorities[0]").value("ROLE_reader"));
    }

    @Test
    void authenticationSucceedsWithEmptyAuthoritiesWhenAuthorityClaimIsAbsent() throws Exception {
        String token = OIDC.issueToken(
            "no-roles-user",
            OIDC.issuerUri(),
            "govaryn-kernel",
            Instant.now(),
            Instant.now().minusSeconds(10),
            Instant.now().plusSeconds(300),
            Map.of("preferred_username", "no-roles")
        );

        mockMvc.perform(get("/api/kernel/whoami").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("no-roles"))
            .andExpect(jsonPath("$.authorities").isEmpty());
    }

    @Test
    void tokenWithWrongAudienceIsRejected() throws Exception {
        String token = OIDC.issueToken("kernel-user", OIDC.issuerUri(), "different-audience", Instant.now(), Instant.now().minusSeconds(10), Instant.now().plusSeconds(300));

        mockMvc.perform(get("/api/kernel/whoami").header("Authorization", "Bearer " + token))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredTokenIsRejected() throws Exception {
        Instant now = Instant.now();
        String token = OIDC.issueToken("kernel-user", OIDC.issuerUri(), "govaryn-kernel", now.minusSeconds(600), now.minusSeconds(660), now.minusSeconds(60));

        mockMvc.perform(get("/api/kernel/whoami").header("Authorization", "Bearer " + token))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void notBeforeInFutureIsRejected() throws Exception {
        Instant now = Instant.now();
        String token = OIDC.issueToken("kernel-user", OIDC.issuerUri(), "govaryn-kernel", now, now.plusSeconds(300), now.plusSeconds(600));

        mockMvc.perform(get("/api/kernel/whoami").header("Authorization", "Bearer " + token))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenWithInvalidSignatureIsRejected() throws Exception {
        String token = OIDC.issueTokenWithDifferentKey("kernel-user", OIDC.issuerUri(), "govaryn-kernel", Instant.now(), Instant.now().minusSeconds(10), Instant.now().plusSeconds(300));

        mockMvc.perform(get("/api/kernel/whoami").header("Authorization", "Bearer " + token))
            .andExpect(status().isUnauthorized());
    }

    private static final class TestOidcServer {
        private HttpServer server;
        private RSAKey signingKey;
        private String issuerUri;

        synchronized void startIfNeeded() {
            if (server != null) {
                return;
            }
            try {
                signingKey = new RSAKeyGenerator(2048).keyID("kernel-test-key").generate();
                server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
                issuerUri = "http://127.0.0.1:" + server.getAddress().getPort();

                server.createContext("/.well-known/openid-configuration", exchange -> {
                    String body = """
                        {
                          "issuer":"%s",
                          "jwks_uri":"%s/jwks"
                        }
                        """.formatted(issuerUri, issuerUri);
                    respondJson(exchange, body);
                });

                server.createContext("/jwks", exchange -> {
                    String body = new JWKSet(signingKey.toPublicJWK()).toString();
                    respondJson(exchange, body);
                });

                server.start();
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to start test OIDC server", ex);
            }
        }

        synchronized void stopIfRunning() {
            if (server != null) {
                server.stop(0);
                server = null;
            }
        }

        String issuerUri() {
            return issuerUri;
        }

        String issueToken(
            String subject,
            String issuer,
            String audience,
            Instant issuedAt,
            Instant notBefore,
            Instant expiresAt
        ) {
            return issueToken(subject, issuer, audience, issuedAt, notBefore, expiresAt, Map.of());
        }

        String issueToken(
            String subject,
            String issuer,
            String audience,
            Instant issuedAt,
            Instant notBefore,
            Instant expiresAt,
            Map<String, Object> extraClaims
        ) {
            try {
                JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                    .jwtID(UUID.randomUUID().toString())
                    .subject(subject)
                    .issuer(issuer)
                    .audience(audience)
                    .issueTime(Date.from(issuedAt))
                    .notBeforeTime(Date.from(notBefore))
                    .expirationTime(Date.from(expiresAt));

                extraClaims.forEach(claimsBuilder::claim);
                JWTClaimsSet claims = claimsBuilder.build();

                SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .type(JOSEObjectType.JWT)
                        .keyID(signingKey.getKeyID())
                        .build(),
                    claims
                );

                JWSSigner signer = new RSASSASigner(signingKey.toPrivateKey());
                jwt.sign(signer);
                return jwt.serialize();
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to issue test JWT", ex);
            }
        }

        String issueTokenWithDifferentKey(
            String subject,
            String issuer,
            String audience,
            Instant issuedAt,
            Instant notBefore,
            Instant expiresAt
        ) {
            try {
                RSAKey differentKey = new RSAKeyGenerator(2048).keyID("wrong-key").generate();
                JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .jwtID(UUID.randomUUID().toString())
                    .subject(subject)
                    .issuer(issuer)
                    .audience(audience)
                    .issueTime(Date.from(issuedAt))
                    .notBeforeTime(Date.from(notBefore))
                    .expirationTime(Date.from(expiresAt))
                    .build();

                SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .type(JOSEObjectType.JWT)
                        .keyID(differentKey.getKeyID())
                        .build(),
                    claims
                );

                JWSSigner signer = new RSASSASigner(differentKey.toPrivateKey());
                jwt.sign(signer);
                return jwt.serialize();
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to issue invalid-signature JWT", ex);
            }
        }

        private static void respondJson(HttpExchange exchange, String body) {
            try {
                byte[] payload = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, payload.length);
                exchange.getResponseBody().write(payload);
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to write OIDC response", ex);
            } finally {
                exchange.close();
            }
        }
    }
}
