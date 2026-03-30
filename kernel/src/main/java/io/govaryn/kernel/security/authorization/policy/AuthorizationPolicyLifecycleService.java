package io.govaryn.kernel.security.authorization.policy;

import io.govaryn.kernel.config.GovarynKernelAuthorizationProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.Optional;

@Service
public class AuthorizationPolicyLifecycleService {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationPolicyLifecycleService.class);

    private final GovarynKernelAuthorizationProperties authorizationProperties;
    private final AuthorizationPolicySourceLoader sourceLoader;
    private final AuthorizationPolicySemanticValidator validator;
    private final ActiveAuthorizationPolicyStore activePolicyStore;

    public AuthorizationPolicyLifecycleService(
        GovarynKernelAuthorizationProperties authorizationProperties,
        AuthorizationPolicySourceLoader sourceLoader,
        AuthorizationPolicySemanticValidator validator,
        ActiveAuthorizationPolicyStore activePolicyStore
    ) {
        this.authorizationProperties = authorizationProperties;
        this.sourceLoader = sourceLoader;
        this.validator = validator;
        this.activePolicyStore = activePolicyStore;
    }

    @PostConstruct
    void initializeAtStartup() {
        if (!authorizationProperties.isEnabled()) {
            log.info("event=authorization_policy_startup_skipped reason=authorization_disabled");
            return;
        }

        AuthorizationPolicyReloadResult result = reloadInternal(true);
        if (!result.success()) {
            throw new IllegalStateException("Authorization policy startup activation failed: " + result.errorMessage());
        }
    }

    public AuthorizationPolicyReloadResult reloadFromConfiguredSource() {
        if (!authorizationProperties.isEnabled()) {
            return AuthorizationPolicyReloadResult.failure("Authorization policy reload is disabled");
        }
        return reloadInternal(false);
    }

    public Optional<ActiveAuthorizationPolicySnapshot> currentActivePolicy() {
        return activePolicyStore.getActivePolicy();
    }

    private AuthorizationPolicyReloadResult reloadInternal(boolean startup) {
        String configuredPath = authorizationProperties.getPolicyPath();
        Path policyPath = Path.of(configuredPath);

        try {
            ParsedPolicySet parsedPolicy = sourceLoader.load(policyPath);
            PolicySetDocument validatedPolicy = validator.validate(parsedPolicy);
            ActiveAuthorizationPolicySnapshot activated = activePolicyStore.activate(validatedPolicy);

            log.info(
                "event=authorization_policy_activated revision={} source={} startup={}",
                sanitizeForLog(activated.revision()),
                sanitizeForLog(policyPath.toAbsolutePath().toString()),
                startup
            );
            return AuthorizationPolicyReloadResult.success(activated.revision());
        } catch (Exception ex) {
            String message = "Failed to load/validate/activate authorization policy from "
                + policyPath.toAbsolutePath() + ": " + sanitizeForLog(ex.getMessage());

            if (startup) {
                log.error(
                    "event=authorization_policy_startup_failed source={} errorType={} errorCause={}",
                    sanitizeForLog(policyPath.toAbsolutePath().toString()),
                    ex.getClass().getSimpleName(),
                    sanitizeForLog(ex.getMessage()),
                    ex
                );
            } else {
                log.warn(
                    "event=authorization_policy_reload_rejected source={} errorType={} errorCause={} activeRevision={}",
                    sanitizeForLog(policyPath.toAbsolutePath().toString()),
                    ex.getClass().getSimpleName(),
                    sanitizeForLog(ex.getMessage()),
                    activePolicyStore.getActivePolicy().map(ActiveAuthorizationPolicySnapshot::revision).orElse("none")
                );
            }
            return AuthorizationPolicyReloadResult.failure(message);
        }
    }

    private static String sanitizeForLog(String value) {
        if (value == null) {
            return "null";
        }
        return value.replaceAll("[\\r\\n\\t\\x00-\\x1F]", " ").trim();
    }
}
