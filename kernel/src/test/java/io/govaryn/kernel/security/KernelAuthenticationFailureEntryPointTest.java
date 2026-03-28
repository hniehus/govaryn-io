package io.govaryn.kernel.security;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;

class KernelAuthenticationFailureEntryPointTest {

    @Test
    void logsMissingBearerTokenCategoryAndReturns401() throws Exception {
        KernelAuthenticationFailureEntryPoint entryPoint = new KernelAuthenticationFailureEntryPoint();
        ListAppender<ILoggingEvent> appender = attachAppender();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/kernel/whoami");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("No credentials"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.getFirst().getFormattedMessage()).contains("category=missing_bearer_token");
    }

    @Test
    void logsAudienceValidationFailureCategoryAndNeverLogsTokenContents() throws Exception {
        KernelAuthenticationFailureEntryPoint entryPoint = new KernelAuthenticationFailureEntryPoint();
        ListAppender<ILoggingEvent> appender = attachAppender();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/kernel/whoami");
        request.addHeader("Authorization", "Bearer header.payload.signature");
        MockHttpServletResponse response = new MockHttpServletResponse();

        BadCredentialsException exception = new BadCredentialsException(
            "Audience claim validation failed for token header.payload.signature"
        );

        entryPoint.commence(request, response, exception);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(appender.list).hasSize(1);
        String logMessage = appender.list.getFirst().getFormattedMessage();
        assertThat(logMessage).contains("category=audience_validation_failure");
        assertThat(logMessage).doesNotContain("header.payload.signature");
        assertThat(logMessage).doesNotContain("Authorization");
    }

    @Test
    void logsProviderOrKeyRetrievalFailureCategory() throws Exception {
        KernelAuthenticationFailureEntryPoint entryPoint = new KernelAuthenticationFailureEntryPoint();
        ListAppender<ILoggingEvent> appender = attachAppender();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/kernel/whoami");
        request.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("Could not retrieve remote JWK set"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.getFirst().getFormattedMessage()).contains("category=provider_or_key_retrieval_failure");
    }

    @Test
    void logsIssuerMismatchCategory() throws Exception {
        KernelAuthenticationFailureEntryPoint entryPoint = new KernelAuthenticationFailureEntryPoint();
        ListAppender<ILoggingEvent> appender = attachAppender();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/kernel/whoami");
        request.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("The iss claim is not valid"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.getFirst().getFormattedMessage()).contains("category=issuer_mismatch");
    }

    private ListAppender<ILoggingEvent> attachAppender() {
        Logger logger = (Logger) org.slf4j.LoggerFactory.getLogger(KernelAuthenticationFailureEntryPoint.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.detachAndStopAllAppenders();
        logger.addAppender(appender);
        logger.setLevel(Level.WARN);
        return appender;
    }
}
