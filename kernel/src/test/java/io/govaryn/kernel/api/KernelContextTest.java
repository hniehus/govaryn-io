package io.govaryn.kernel.api;

import io.govaryn.kernel.config.KernelEnvironment;
import io.govaryn.kernel.security.authorization.model.AuthorizationDecision;
import io.govaryn.kernel.security.authorization.model.AuthorizationSubject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KernelContextTest {

    @Test
    void legacyConstructorRemainsBackwardCompatible() {
        KernelContext context = new KernelContext("kernel-a", KernelEnvironment.DEV, "1.2.0");

        assertThat(context.kernelId()).isEqualTo("kernel-a");
        assertThat(context.environment()).isEqualTo(KernelEnvironment.DEV);
        assertThat(context.kernelVersion()).isEqualTo("1.2.0");
        assertThat(context.configurationManager()).isNull();
        assertThat(context.authorizationService()).isNull();
        assertThat(context.currentSecurityContext()).isNull();
    }

    @Test
    void constructorWithAuthorizationServiceDefaultsCurrentSecurityContextToNull() {
        KernelAuthorizationService authorizationService = new KernelAuthorizationService() {
            @Override
            public AuthorizationDecision authorize(
                AuthorizationSubject subject,
                KernelAuthorizationOperation operation
            ) {
                return null;
            }

            @Override
            public AuthorizationDecision authorize(
                AuthorizationSubject subject,
                String action,
                String resourceType,
                String resourceId,
                Map<String, String> context
            ) {
                return null;
            }
        };

        KernelContext context = new KernelContext(
            "kernel-a",
            KernelEnvironment.DEV,
            "1.2.0",
            null,
            authorizationService
        );

        assertThat(context.authorizationService()).isSameAs(authorizationService);
        assertThat(context.currentSecurityContext()).isNull();
    }

    @Test
    void canonicalConstructorAcceptsCurrentSecurityContext() {
        KernelCurrentSecurityContext currentSecurityContext = () -> java.util.Optional.empty();

        KernelContext context = new KernelContext(
            "kernel-a",
            KernelEnvironment.DEV,
            "1.2.0",
            null,
            null,
            currentSecurityContext
        );

        assertThat(context.currentSecurityContext()).isSameAs(currentSecurityContext);
    }
}
