package io.govaryn.kernel.security.authorization.framework;

import io.govaryn.kernel.security.authorization.framework.model.AuthorizationAction;
import io.govaryn.kernel.security.authorization.framework.model.AuthorizationDecision;
import io.govaryn.kernel.security.authorization.framework.model.AuthorizationRequest;
import io.govaryn.kernel.security.authorization.framework.model.DenyReason;
import io.govaryn.kernel.security.authorization.framework.model.SecurityContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
@DisplayName("KernelAuthorizationService")
class KernelAuthorizationServiceTest {

    @Test
    @DisplayName("Allows request when registered evaluator permits")
    void allowsRequestWhenRegisteredEvaluatorPermits() {
        InMemoryResourcePolicyRegistry policyRegistry = new InMemoryResourcePolicyRegistry();
        policyRegistry.register(new ResourcePolicyRegistration(
            "orders",
            "order",
            Set.of(AuthorizationAction.READ),
            request -> AuthorizationDecision.allow("orders.read-policy")
        ));

        KernelAuthorizationService service = new KernelAuthorizationService(
            policyRegistry,
            new AuthorizationAuditLogger()
        );

        AuthorizationDecision decision = service.authorize(request(
            "orders",
            AuthorizationAction.READ,
            "order",
            "order-1"
        ));

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.denyReason()).isNull();
        assertThat(decision.policySource()).isEqualTo("orders.read-policy");
    }

    @Test
    @DisplayName("Denies request when registered evaluator denies")
    void deniesRequestWhenRegisteredEvaluatorDenies() {
        InMemoryResourcePolicyRegistry policyRegistry = new InMemoryResourcePolicyRegistry();
        policyRegistry.register(new ResourcePolicyRegistration(
            "orders",
            "order",
            Set.of(AuthorizationAction.UPDATE),
            request -> AuthorizationDecision.deny(DenyReason.RECORD_ACCESS_DENIED, "orders.record-policy")
        ));

        KernelAuthorizationService service = new KernelAuthorizationService(
            policyRegistry,
            new AuthorizationAuditLogger()
        );

        AuthorizationDecision decision = service.authorize(request(
            "orders",
            AuthorizationAction.UPDATE,
            "order",
            "order-2"
        ));

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.denyReason()).isEqualTo(DenyReason.RECORD_ACCESS_DENIED);
        assertThat(decision.policySource()).isEqualTo("orders.record-policy");
    }

    @Test
    @DisplayName("Logs deny decisions in structured audit format")
    void logsDenyDecisionsInStructuredAuditFormat(CapturedOutput output) {
        InMemoryResourcePolicyRegistry policyRegistry = new InMemoryResourcePolicyRegistry();
        policyRegistry.register(new ResourcePolicyRegistration(
            "orders",
            "order",
            Set.of(AuthorizationAction.DELETE),
            request -> AuthorizationDecision.deny(DenyReason.RESOURCE_ACCESS_DENIED, "orders.delete-policy")
        ));

        KernelAuthorizationService service = new KernelAuthorizationService(
            policyRegistry,
            new AuthorizationAuditLogger()
        );

        MDC.put("requestId", "req-123");
        try {
            service.authorize(request(
                "orders",
                AuthorizationAction.DELETE,
                "order",
                "order-9"
            ));
        } finally {
            MDC.clear();
        }

        String logs = output.getOut() + output.getErr();
        assertThat(logs)
            .contains("event=authorization_deny_audit")
            .contains("userId=user-1")
            .contains("tenantId=tenant-1")
            .contains("module=orders")
            .contains("resourceType=order")
            .contains("action=DELETE")
            .contains("resourceId=order-9")
            .contains("decision=DENY")
            .contains("denyReason=RESOURCE_ACCESS_DENIED")
            .contains("requestRef=req-123");
    }

    private static AuthorizationRequest request(
        String moduleId,
        AuthorizationAction action,
        String resourceType,
        String resourceId
    ) {
        return new AuthorizationRequest(
            new SecurityContext(
                "user-1",
                "tenant-1",
                List.of("ROLE_ops"),
                Map.of("scope", "orders:full"),
                Map.of("issuer", "https://idp.example.com/realms/main")
            ),
            moduleId,
            action,
            resourceType,
            resourceId,
            Map.of("scope", "global")
        );
    }
}
