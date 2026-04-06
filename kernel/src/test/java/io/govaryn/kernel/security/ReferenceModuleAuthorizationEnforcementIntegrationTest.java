package io.govaryn.kernel.security;

import io.govaryn.kernel.security.authorization.framework.KernelAuthorizationEnforcer;
import io.govaryn.kernel.security.authorization.framework.ResourcePolicyRegistry;
import io.govaryn.kernel.security.authorization.framework.model.AuthorizationAction;
import io.govaryn.modules.examples.ReferenceAuthorizationContract;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ActiveProfiles("module-reference-authz-e2e")
@Import({
    ReferenceModuleAuthorizationEnforcementIntegrationTest.ReferenceBackendFixtureConfiguration.class,
    ReferenceModuleAuthorizationEnforcementIntegrationTest.TestSecurityBeans.class
})
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(properties = {
    "govaryn.kernel.id=test-kernel",
    "govaryn.kernel.environment=dev",
    "spring.application.version=1.2.0",
    "spring.main.allow-bean-definition-overriding=true",
    "govaryn.kernel.security.enabled=true",
    "govaryn.kernel.security.issuer-uri=https://idp.example.com/realms/main",
    "govaryn.kernel.security.audience=govaryn-kernel",
    "govaryn.kernel.security.authority-claim=roles",
    "govaryn.kernel.security.authority-prefix=ROLE_",
    "govaryn.kernel.security.public-paths[0]=/health"
})
class ReferenceModuleAuthorizationEnforcementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ResourcePolicyRegistry policyRegistry;

    @Autowired
    private ReferenceDocumentFixtureService fixtureService;

    @BeforeEach
    void resetFixtureData() {
        fixtureService.reset();
    }

    @Test
    @DisplayName("Reference module policy is registered in kernel registry")
    void referenceModulePolicyIsRegisteredInKernelRegistry() {
        assertThat(policyRegistry.resolve(
            ReferenceAuthorizationContract.MODULE_ID,
            ReferenceAuthorizationContract.RESOURCE_TYPE
        )).isPresent();
    }

    @Test
    @DisplayName("Read and list are allowed only within effective tenant and scope")
    void readAndListAreAllowedOnlyWithinEffectiveTenantAndScope() throws Exception {
        mockMvc.perform(get(listPath("tenant-1")).header("Authorization", "Bearer tenant1-read-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value("doc-1"))
            .andExpect(jsonPath("$[0].tenantId").value("tenant-1"))
            .andExpect(jsonPath("$[0].value").value("tenant-1-initial"));

        mockMvc.perform(get(readPath("tenant-1", "doc-1")).header("Authorization", "Bearer tenant1-read-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("doc-1"))
            .andExpect(jsonPath("$.tenantId").value("tenant-1"))
            .andExpect(jsonPath("$.value").value("tenant-1-initial"));

        mockMvc.perform(get(listPath("tenant-2")).header("Authorization", "Bearer tenant1-read-token"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
            .andExpect(jsonPath("$.reason").value("ACCESS_DENIED"));

        mockMvc.perform(get(listPath("tenant-1")).header("Authorization", "Bearer tenant1-no-scope-token"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
            .andExpect(jsonPath("$.reason").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("Update within effective scope succeeds through kernel enforcement")
    void updateWithinEffectiveScopeSucceedsThroughKernelEnforcement() throws Exception {
        mockMvc.perform(put(readPath("tenant-1", "doc-1"))
                .header("Authorization", "Bearer tenant1-write-token")
                .contentType(APPLICATION_JSON)
                .content("{\"value\":\"tenant-1-updated\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("doc-1"))
            .andExpect(jsonPath("$.tenantId").value("tenant-1"))
            .andExpect(jsonPath("$.value").value("tenant-1-updated"));

        mockMvc.perform(get(readPath("tenant-1", "doc-1")).header("Authorization", "Bearer tenant1-read-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.value").value("tenant-1-updated"));
    }

    @Test
    @DisplayName("Update without write scope is denied and logged as structured audit")
    void updateWithoutWriteScopeIsDeniedAndLoggedAsStructuredAudit(CapturedOutput output) throws Exception {
        mockMvc.perform(put(readPath("tenant-2", "doc-2"))
                .header("Authorization", "Bearer tenant2-read-token")
                .contentType(APPLICATION_JSON)
                .content("{\"value\":\"blocked\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
            .andExpect(jsonPath("$.reason").value("ACCESS_DENIED"));

        mockMvc.perform(get(readPath("tenant-2", "doc-2")).header("Authorization", "Bearer tenant2-read-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.value").value("tenant-2-initial"));

        String logs = output.getOut() + output.getErr();
        assertThat(logs)
            .contains("event=authorization_deny_audit")
            .contains("userId=subject-tenant2-read")
            .contains("tenantId=tenant-2")
            .contains("module=" + ReferenceAuthorizationContract.MODULE_ID)
            .contains("resourceType=" + ReferenceAuthorizationContract.RESOURCE_TYPE)
            .contains("action=UPDATE")
            .contains("resourceId=doc-2")
            .contains("decision=DENY")
            .contains("denyReason=RECORD_ACCESS_DENIED");
    }

    private static String listPath(String tenantId) {
        return "/api/reference-authz/tenants/" + tenantId + "/documents";
    }

    private static String readPath(String tenantId, String documentId) {
        return "/api/reference-authz/tenants/" + tenantId + "/documents/" + documentId;
    }

    @TestConfiguration
    static class ReferenceBackendFixtureConfiguration {
        @Bean
        ReferenceDocumentFixtureService referenceDocumentFixtureService() {
            return new ReferenceDocumentFixtureService();
        }

        @Bean
        ReferenceDocumentFixtureController referenceDocumentFixtureController(
            ReferenceDocumentFixtureService fixtureService,
            KernelAuthorizationEnforcer authorizationEnforcer
        ) {
            return new ReferenceDocumentFixtureController(fixtureService, authorizationEnforcer);
        }
    }

    @RestController
    @RequestMapping("/api/reference-authz/tenants/{tenantId}/documents")
    static class ReferenceDocumentFixtureController {

        private final ReferenceDocumentFixtureService fixtureService;
        private final KernelAuthorizationEnforcer authorizationEnforcer;

        ReferenceDocumentFixtureController(
            ReferenceDocumentFixtureService fixtureService,
            KernelAuthorizationEnforcer authorizationEnforcer
        ) {
            this.fixtureService = fixtureService;
            this.authorizationEnforcer = authorizationEnforcer;
        }

        @GetMapping
        ResponseEntity<List<ReferenceDocument>> list(@PathVariable String tenantId) {
            authorizationEnforcer.enforce(
                ReferenceAuthorizationContract.MODULE_ID,
                AuthorizationAction.LIST,
                ReferenceAuthorizationContract.RESOURCE_TYPE,
                null,
                Map.of(ReferenceAuthorizationContract.ATTRIBUTE_TENANT_ID, tenantId)
            );
            return ResponseEntity.ok(fixtureService.list(tenantId));
        }

        @GetMapping("/{documentId}")
        ResponseEntity<ReferenceDocument> readOne(@PathVariable String tenantId, @PathVariable String documentId) {
            authorizationEnforcer.enforce(
                ReferenceAuthorizationContract.MODULE_ID,
                AuthorizationAction.READ,
                ReferenceAuthorizationContract.RESOURCE_TYPE,
                documentId,
                Map.of(ReferenceAuthorizationContract.ATTRIBUTE_TENANT_ID, tenantId)
            );
            return fixtureService.readOne(tenantId, documentId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Document not found"));
        }

        @PutMapping("/{documentId}")
        ResponseEntity<ReferenceDocument> update(
            @PathVariable String tenantId,
            @PathVariable String documentId,
            @Valid @RequestBody ReferenceDocumentUpdateRequest request
        ) {
            authorizationEnforcer.enforce(
                ReferenceAuthorizationContract.MODULE_ID,
                AuthorizationAction.UPDATE,
                ReferenceAuthorizationContract.RESOURCE_TYPE,
                documentId,
                Map.of(ReferenceAuthorizationContract.ATTRIBUTE_TENANT_ID, tenantId)
            );
            return fixtureService.update(tenantId, documentId, request.value())
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Document not found"));
        }
    }

    static final class ReferenceDocumentFixtureService {
        private final ConcurrentHashMap<String, ReferenceDocument> documents = new ConcurrentHashMap<>();

        ReferenceDocumentFixtureService() {
            reset();
        }

        void reset() {
            documents.clear();
            documents.put("doc-1", new ReferenceDocument("doc-1", "tenant-1", "tenant-1-initial"));
            documents.put("doc-2", new ReferenceDocument("doc-2", "tenant-2", "tenant-2-initial"));
        }

        List<ReferenceDocument> list(String tenantId) {
            return documents.values().stream()
                .filter(document -> tenantId.equals(document.tenantId()))
                .sorted(Comparator.comparing(ReferenceDocument::id))
                .toList();
        }

        Optional<ReferenceDocument> readOne(String tenantId, String documentId) {
            ReferenceDocument document = documents.get(documentId);
            if (document == null || !tenantId.equals(document.tenantId())) {
                return Optional.empty();
            }
            return Optional.of(document);
        }

        Optional<ReferenceDocument> update(String tenantId, String documentId, String value) {
            ReferenceDocument existing = documents.get(documentId);
            if (existing == null || !tenantId.equals(existing.tenantId())) {
                return Optional.empty();
            }
            ReferenceDocument updated = new ReferenceDocument(existing.id(), existing.tenantId(), value.trim());
            documents.put(updated.id(), updated);
            return Optional.of(updated);
        }
    }

    record ReferenceDocument(
        String id,
        String tenantId,
        String value
    ) {
    }

    record ReferenceDocumentUpdateRequest(
        @NotBlank(message = "value must not be blank")
        String value
    ) {
    }

    @TestConfiguration
    static class TestSecurityBeans {
        @Bean("kernelJwtDecoder")
        @Primary
        JwtDecoder kernelJwtDecoder() {
            return token -> switch (token) {
                case "tenant1-read-token" -> jwt(
                    token,
                    "subject-tenant1-read",
                    "https://idp.example.com/realms/main",
                    List.of("govaryn-kernel"),
                    "tenant1-reader",
                    List.of("viewer"),
                    "reference.documents.read",
                    "tenant-1"
                );
                case "tenant1-write-token" -> jwt(
                    token,
                    "subject-tenant1-write",
                    "https://idp.example.com/realms/main",
                    List.of("govaryn-kernel"),
                    "tenant1-writer",
                    List.of("editor"),
                    "reference.documents.write",
                    "tenant-1"
                );
                case "tenant1-no-scope-token" -> jwt(
                    token,
                    "subject-tenant1-noscope",
                    "https://idp.example.com/realms/main",
                    List.of("govaryn-kernel"),
                    "tenant1-noscope",
                    List.of("viewer"),
                    "",
                    "tenant-1"
                );
                case "tenant2-read-token" -> jwt(
                    token,
                    "subject-tenant2-read",
                    "https://idp.example.com/realms/main",
                    List.of("govaryn-kernel"),
                    "tenant2-reader",
                    List.of("viewer"),
                    "reference.documents.read",
                    "tenant-2"
                );
                default -> jwt(
                    token,
                    "subject-default",
                    "https://idp.example.com/realms/main",
                    List.of("govaryn-kernel"),
                    "default",
                    List.of("viewer"),
                    "",
                    "tenant-1"
                );
            };
        }

        private static Jwt jwt(
            String token,
            String subject,
            String issuer,
            List<String> audience,
            String username,
            List<String> roles,
            String scope,
            String tenantId
        ) {
            return Jwt.withTokenValue(token)
                .header("alg", "none")
                .claim("sub", subject)
                .claim("iss", issuer)
                .claim("aud", audience)
                .claim("preferred_username", username)
                .claim("roles", roles)
                .claim("scope", scope)
                .claim("tenant_id", tenantId)
                .issuedAt(Instant.now().minusSeconds(5))
                .notBefore(Instant.now().minusSeconds(5))
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        }
    }
}
