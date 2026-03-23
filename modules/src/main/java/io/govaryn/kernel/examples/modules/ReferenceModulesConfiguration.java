package io.govaryn.kernel.examples.modules;

import io.govaryn.kernel.api.KernelModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class ReferenceModulesConfiguration {

    @Bean
    @Profile("module-example-minimal")
    KernelModule minimalReferenceModule() {
        return new MinimalReferenceModule();
    }

    @Bean
    @Profile("module-example-incompatible-api")
    KernelModule incompatibleApiVersionModule() {
        return new IncompatibleApiVersionModule();
    }

    @Bean
    @Profile("module-example-duplicate-id")
    KernelModule duplicateIdModuleA() {
        return new DuplicateIdModuleA();
    }

    @Bean
    @Profile("module-example-duplicate-id")
    KernelModule duplicateIdModuleB() {
        return new DuplicateIdModuleB();
    }

    @Bean
    @Profile("module-example-failing-init")
    KernelModule failingInitializationModule() {
        return new FailingInitializationModule();
    }
}
