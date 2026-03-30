package io.govaryn.kernel.security.authorization.policy;

import io.govaryn.kernel.config.GovarynKernelAuthorizationProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AuthorizationPolicyLifecycleService Tests")
class AuthorizationPolicyLifecycleServiceTest {

    @Test
    @DisplayName("Should load valid policy at startup")
    void shouldLoadValidPolicyAtStartup() throws IOException {
        AuthorizationPolicyLifecycleService service = serviceFor(policyPath("policy-valid.yaml"));

        service.initializeAtStartup();

        assertTrue(service.currentActivePolicy().isPresent());
        assertEquals("2026-03-30.v1", service.currentActivePolicy().orElseThrow().revision());
    }

    @Test
    @DisplayName("Should fail startup on invalid policy and keep no active policy")
    void shouldFailStartupOnInvalidPolicyAndKeepNoActivePolicy() throws IOException {
        AuthorizationPolicyLifecycleService service = serviceFor(policyPath("policy-invalid.yaml"));

        IllegalStateException ex = assertThrows(IllegalStateException.class, service::initializeAtStartup);

        assertTrue(ex.getMessage().contains("startup activation failed"));
        assertTrue(service.currentActivePolicy().isEmpty());
    }

    @Test
    @DisplayName("Should preserve last known valid policy when reload fails")
    void shouldPreserveLastKnownValidPolicyWhenReloadFails() throws IOException {
        GovarynKernelAuthorizationProperties properties = enabledProperties(policyPath("policy-valid.yaml"));
        AuthorizationPolicyLifecycleService service = new AuthorizationPolicyLifecycleService(
            properties,
            new YamlAuthorizationPolicyParser(),
            new AuthorizationPolicyValidator(),
            new InMemoryActiveAuthorizationPolicyStore()
        );

        service.initializeAtStartup();
        String revisionBefore = service.currentActivePolicy().orElseThrow().revision();

        properties.setPolicyPath(policyPath("policy-empty-actions.yaml"));
        AuthorizationPolicyReloadResult reloadResult = service.reloadFromConfiguredSource();

        assertFalse(reloadResult.success());
        assertEquals(revisionBefore, service.currentActivePolicy().orElseThrow().revision());
    }

    @Test
    @DisplayName("Should update revision only on successful activation")
    void shouldUpdateRevisionOnlyOnSuccessfulActivation() throws IOException {
        GovarynKernelAuthorizationProperties properties = enabledProperties(policyPath("policy-valid.yaml"));
        AuthorizationPolicyLifecycleService service = new AuthorizationPolicyLifecycleService(
            properties,
            new YamlAuthorizationPolicyParser(),
            new AuthorizationPolicyValidator(),
            new InMemoryActiveAuthorizationPolicyStore()
        );

        service.initializeAtStartup();
        String revisionV1 = service.currentActivePolicy().orElseThrow().revision();

        properties.setPolicyPath(policyPath("policy-duplicate-rule-id.yaml"));
        AuthorizationPolicyReloadResult failedReload = service.reloadFromConfiguredSource();
        assertFalse(failedReload.success());
        assertEquals(revisionV1, service.currentActivePolicy().orElseThrow().revision());

        properties.setPolicyPath(policyPath("policy-valid-v2.yaml"));
        AuthorizationPolicyReloadResult successfulReload = service.reloadFromConfiguredSource();
        assertTrue(successfulReload.success());
        assertEquals("2026-03-30.v2", successfulReload.revision());
        assertEquals("2026-03-30.v2", service.currentActivePolicy().orElseThrow().revision());
    }

    private static AuthorizationPolicyLifecycleService serviceFor(String policyPath) {
        GovarynKernelAuthorizationProperties properties = enabledProperties(policyPath);
        return new AuthorizationPolicyLifecycleService(
            properties,
            new YamlAuthorizationPolicyParser(),
            new AuthorizationPolicyValidator(),
            new InMemoryActiveAuthorizationPolicyStore()
        );
    }

    private static GovarynKernelAuthorizationProperties enabledProperties(String policyPath) {
        GovarynKernelAuthorizationProperties properties = new GovarynKernelAuthorizationProperties();
        properties.setEnabled(true);
        properties.setPolicyPath(policyPath);
        return properties;
    }

    private static String policyPath(String name) throws IOException {
        return new ClassPathResource("security/authorization/policy/" + name).getFile().toPath().toString();
    }
}
