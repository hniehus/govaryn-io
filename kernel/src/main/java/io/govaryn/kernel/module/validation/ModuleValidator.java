package io.govaryn.kernel.module.validation;

import io.govaryn.kernel.module.discovery.ModuleDiscoveryCandidate;

import java.util.List;

public interface ModuleValidator {

    List<ModuleValidationReport> validate(List<ModuleDiscoveryCandidate> candidates, String runningKernelApiVersion);
}
