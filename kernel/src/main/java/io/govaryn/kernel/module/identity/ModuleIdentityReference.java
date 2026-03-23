package io.govaryn.kernel.module.identity;

import io.govaryn.kernel.module.discovery.ModuleDiscoverySource;

public record ModuleIdentityReference(
    String moduleId,
    String moduleName,
    String moduleVersion,
    ModuleDiscoverySource source,
    String origin
) {
}
