package io.govaryn.kernel.internal;

import io.govaryn.kernel.api.KernelContext;
import io.govaryn.kernel.api.KernelModule;
import io.govaryn.kernel.config.GovarynKernelProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class ModuleOrchestrator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ModuleOrchestrator.class);

    private final List<KernelModule> modules;
    private final GovarynKernelProperties properties;
    private final String kernelVersion;

    public ModuleOrchestrator(ObjectProvider<KernelModule> modules,
                              GovarynKernelProperties properties,
                              @Value("${spring.application.version:${project.version:unknown}}") String kernelVersion) {
        this.modules = modules.orderedStream().sorted(Comparator.comparingInt(KernelModule::order)).toList();
        this.properties = properties;
        this.kernelVersion = kernelVersion;
    }

    @Override
    public void run(ApplicationArguments args) {
        KernelContext context = new KernelContext(properties.getId(), properties.getEnvironment(), kernelVersion);

        for (KernelModule module : modules) {
            log.info("Initializing kernel module: {}", module.moduleName());
            module.init(context);
        }

        for (KernelModule module : modules) {
            log.info("Starting kernel module: {}", module.moduleName());
            module.start();
        }

        log.info("Govaryn Kernel started with {} module(s)", modules.size());
    }
}
