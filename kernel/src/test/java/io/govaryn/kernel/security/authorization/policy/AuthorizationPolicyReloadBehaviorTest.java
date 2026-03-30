package io.govaryn.kernel.security.authorization.policy;

import io.govaryn.kernel.config.GovarynKernelAuthorizationProperties;
import io.govaryn.kernel.security.authorization.AuthorizationDecisionLogger;
import io.govaryn.kernel.security.authorization.KernelPolicyDecisionPoint;
import io.govaryn.kernel.security.authorization.model.AuthorizationDecision;
import io.govaryn.kernel.security.authorization.model.AuthorizationDecisionResult;
import io.govaryn.kernel.security.authorization.model.AuthorizationRequest;
import io.govaryn.kernel.security.authorization.model.AuthorizationSubject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Authorization Policy Reload Behavior")
class AuthorizationPolicyReloadBehaviorTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Successful reload changes subsequent authorization decisions")
    void successfulReloadChangesSubsequentAuthorizationDecisions() throws IOException {
        Path policyPath = tempDir.resolve("authorization-policy.yaml");
        write(policyPath, permitPolicy("2026-03-30.v1"));

        TestHarness harness = harness(policyPath);
        AuthorizationDecision beforeReload = harness.pdp.authorize(readModuleRequest());
        assertEquals(AuthorizationDecisionResult.PERMIT, beforeReload.result());
        assertEquals("2026-03-30.v1", harness.service.currentActivePolicy().orElseThrow().revision());

        write(policyPath, denyPolicy("2026-03-30.v2"));
        AuthorizationPolicyReloadResult reload = harness.service.reloadFromConfiguredSource();

        assertTrue(reload.success());
        assertEquals("2026-03-30.v2", reload.revision());
        assertEquals("2026-03-30.v2", harness.service.currentActivePolicy().orElseThrow().revision());

        AuthorizationDecision afterReload = harness.pdp.authorize(readModuleRequest());
        assertEquals(AuthorizationDecisionResult.DENY, afterReload.result());
    }

    @Test
    @DisplayName("Failed reload preserves previously active policy decisions")
    void failedReloadPreservesPreviouslyActivePolicyDecisions() throws IOException {
        Path policyPath = tempDir.resolve("authorization-policy.yaml");
        write(policyPath, permitPolicy("2026-03-30.v1"));

        TestHarness harness = harness(policyPath);
        assertEquals(AuthorizationDecisionResult.PERMIT, harness.pdp.authorize(readModuleRequest()).result());
        String revisionBefore = harness.service.currentActivePolicy().orElseThrow().revision();

        write(policyPath, invalidPolicy("2026-03-30.invalid"));
        AuthorizationPolicyReloadResult reload = harness.service.reloadFromConfiguredSource();

        assertFalse(reload.success());
        assertEquals(revisionBefore, harness.service.currentActivePolicy().orElseThrow().revision());
        assertEquals(AuthorizationDecisionResult.PERMIT, harness.pdp.authorize(readModuleRequest()).result());
    }

    @Test
    @DisplayName("Policy revision updates only on successful reload activation")
    void policyRevisionUpdatesOnlyOnSuccessfulReloadActivation() throws IOException {
        Path policyPath = tempDir.resolve("authorization-policy.yaml");
        write(policyPath, permitPolicy("2026-03-30.v1"));

        TestHarness harness = harness(policyPath);
        assertEquals("2026-03-30.v1", harness.service.currentActivePolicy().orElseThrow().revision());

        write(policyPath, invalidPolicy("2026-03-30.invalid"));
        AuthorizationPolicyReloadResult failedReload = harness.service.reloadFromConfiguredSource();
        assertFalse(failedReload.success());
        assertEquals("2026-03-30.v1", harness.service.currentActivePolicy().orElseThrow().revision());

        write(policyPath, denyPolicy("2026-03-30.v2"));
        AuthorizationPolicyReloadResult successfulReload = harness.service.reloadFromConfiguredSource();
        assertTrue(successfulReload.success());
        assertEquals("2026-03-30.v2", successfulReload.revision());
        assertEquals("2026-03-30.v2", harness.service.currentActivePolicy().orElseThrow().revision());
    }

    private TestHarness harness(Path policyPath) {
        GovarynKernelAuthorizationProperties properties = new GovarynKernelAuthorizationProperties();
        properties.setEnabled(true);
        properties.setPolicyPath(policyPath.toString());

        InMemoryActiveAuthorizationPolicyStore activeStore = new InMemoryActiveAuthorizationPolicyStore();
        AuthorizationPolicyLifecycleService service = new AuthorizationPolicyLifecycleService(
            properties,
            new YamlAuthorizationPolicyParser(),
            new AuthorizationPolicyValidator(),
            activeStore
        );
        service.initializeAtStartup();

        KernelPolicyDecisionPoint pdp = new KernelPolicyDecisionPoint(activeStore, new AuthorizationDecisionLogger());
        return new TestHarness(service, pdp);
    }

    private static AuthorizationRequest readModuleRequest() {
        return new AuthorizationRequest(
            new AuthorizationSubject("subject-1", List.of("ROLE_admin"), Map.of()),
            "read",
            "module",
            null,
            Map.of()
        );
    }

    private static void write(Path path, String yaml) throws IOException {
        Files.writeString(path, yaml);
    }

    private static String permitPolicy(String revision) {
        return """
            policySetRevision: "%s"
            rules:
              - id: permit-read-module
                effect: PERMIT
                subject:
                  roles:
                    - ROLE_admin
                actions:
                  - read
                resourceTypes:
                  - module
            """.formatted(revision);
    }

    private static String denyPolicy(String revision) {
        return """
            policySetRevision: "%s"
            rules:
              - id: deny-read-module
                effect: DENY
                subject:
                  roles:
                    - ROLE_admin
                actions:
                  - read
                resourceTypes:
                  - module
            """.formatted(revision);
    }

    private static String invalidPolicy(String revision) {
        return """
            policySetRevision: "%s"
            rules:
              - id: invalid-empty-actions
                effect: PERMIT
                subject:
                  roles:
                    - ROLE_admin
                actions: []
                resourceTypes:
                  - module
            """.formatted(revision);
    }

    private record TestHarness(
        AuthorizationPolicyLifecycleService service,
        KernelPolicyDecisionPoint pdp
    ) {
    }
}
