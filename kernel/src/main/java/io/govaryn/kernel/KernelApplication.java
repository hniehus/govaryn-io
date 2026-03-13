package io.govaryn.kernel;

import io.govaryn.kernel.config.GovarynKernelProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan(basePackageClasses = GovarynKernelProperties.class)
public class KernelApplication {

    public static void main(String[] args) {
        SpringApplication.run(KernelApplication.class, args);
    }
}
