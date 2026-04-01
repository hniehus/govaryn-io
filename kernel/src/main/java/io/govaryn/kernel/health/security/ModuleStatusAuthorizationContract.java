package io.govaryn.kernel.health.security;

/**
 * Contract constants for the protected module status endpoint.
 */
public final class ModuleStatusAuthorizationContract {

    public static final String MODULE_ID = "kernel-module-status";
    public static final String RESOURCE_TYPE = "module-status";
    public static final String SCOPE_READ = "module.status:read";

    private ModuleStatusAuthorizationContract() {
    }
}
