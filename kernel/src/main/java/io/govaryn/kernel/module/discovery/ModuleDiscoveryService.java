package io.govaryn.kernel.module.discovery;

import java.util.List;

public interface ModuleDiscoveryService {

    List<ModuleDiscoveryCandidate> discover();
}
