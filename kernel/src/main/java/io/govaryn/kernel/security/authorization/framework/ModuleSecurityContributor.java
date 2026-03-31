package io.govaryn.kernel.security.authorization.framework;

/**
 * Module SPI for registering module-level and record-level authorization rules.
 */
public interface ModuleSecurityContributor {

    String moduleId();

    void contribute(ModuleSecurityRegistry registry);
}
