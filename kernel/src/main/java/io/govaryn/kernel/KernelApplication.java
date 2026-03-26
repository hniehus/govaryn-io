package io.govaryn.kernel;

import io.govaryn.kernel.config.ConfigurationManager;
import io.govaryn.kernel.config.GovarynKernelProperties;
import io.govaryn.modules.examples.ReferenceModulesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@ConfigurationPropertiesScan(basePackageClasses = GovarynKernelProperties.class)
@Import(ReferenceModulesConfiguration.class)
public class KernelApplication {

    private static final Logger logger = LoggerFactory.getLogger(KernelApplication.class);

    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(KernelApplication.class, args);

        // Initialize ConfigurationManager after Spring context is ready
        try {
            ConfigurationManager configManager = context.getBean(ConfigurationManager.class);
            configManager.initialize();
            logger.info("Govaryn Kernel started successfully with configuration: {}",
                configManager.getStatusSummary());
        } catch (Exception e) {
            logger.error("Failed to initialize ConfigurationManager. Kernel startup failed.", e);
            System.exit(1);
        }
    }
}
