package io.govaryn.kernel.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class GovarynKernelSecurityPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(TestConfig.class);

    @Test
    void startsWhenSecurityIsDisabled() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            GovarynKernelSecurityProperties properties = context.getBean(GovarynKernelSecurityProperties.class);
            assertThat(properties.isEnabled()).isFalse();
        });
    }

    @Test
    void bindsConfiguredSecurityProperties() {
        contextRunner.withPropertyValues(
            "govaryn.kernel.security.enabled=true",
            "govaryn.kernel.security.issuer-uri=https://idp.example.com/realms/main",
            "govaryn.kernel.security.audience=govaryn-kernel",
            "govaryn.kernel.security.public-paths[0]=/actuator/health",
            "govaryn.kernel.security.public-paths[1]=/public/**",
            "govaryn.kernel.security.authority-claim=roles",
            "govaryn.kernel.security.authority-prefix=ROLE_"
        ).run(context -> {
            assertThat(context).hasNotFailed();

            GovarynKernelSecurityProperties properties = context.getBean(GovarynKernelSecurityProperties.class);
            assertThat(properties.isEnabled()).isTrue();
            assertThat(properties.getIssuerUri()).isEqualTo("https://idp.example.com/realms/main");
            assertThat(properties.getAudience()).isEqualTo("govaryn-kernel");
            assertThat(properties.getPublicPaths()).containsExactly("/actuator/health", "/public/**");
            assertThat(properties.getAuthorityClaim()).isEqualTo("roles");
            assertThat(properties.getAuthorityPrefix()).isEqualTo("ROLE_");
        });
    }

    @Test
    void failsStartupWhenSecurityEnabledAndIssuerUriMissing() {
        contextRunner.withPropertyValues(
            "govaryn.kernel.security.enabled=true",
            "govaryn.kernel.security.audience=govaryn-kernel"
        ).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                .hasStackTraceContaining("govaryn.kernel.security.issuer-uri must be configured");
        });
    }

    @Test
    void failsStartupWhenSecurityEnabledAndAudienceMissing() {
        contextRunner.withPropertyValues(
            "govaryn.kernel.security.enabled=true",
            "govaryn.kernel.security.issuer-uri=https://idp.example.com/realms/main"
        ).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                .hasStackTraceContaining("govaryn.kernel.security.audience must be configured");
        });
    }

    @Test
    void failsStartupWhenSecurityEnabledAndIssuerUriInvalid() {
        contextRunner.withPropertyValues(
            "govaryn.kernel.security.enabled=true",
            "govaryn.kernel.security.issuer-uri=not-a-valid-uri",
            "govaryn.kernel.security.audience=govaryn-kernel"
        ).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                .hasStackTraceContaining("govaryn.kernel.security.issuer-uri must be a valid absolute URI");
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(GovarynKernelSecurityProperties.class)
    static class TestConfig {
    }
}
