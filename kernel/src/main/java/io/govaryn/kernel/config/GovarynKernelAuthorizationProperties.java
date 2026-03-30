package io.govaryn.kernel.config;

import jakarta.validation.constraints.AssertTrue;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "govaryn.kernel.authorization")
@Validated
public class GovarynKernelAuthorizationProperties {

    private boolean enabled = false;
    private String policyPath;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPolicyPath() {
        return policyPath;
    }

    public void setPolicyPath(String policyPath) {
        this.policyPath = policyPath;
    }

    @AssertTrue(message = "govaryn.kernel.authorization.policy-path must be configured when govaryn.kernel.authorization.enabled=true")
    boolean isPolicyPathPresentWhenEnabled() {
        return !enabled || (policyPath != null && !policyPath.isBlank());
    }
}
