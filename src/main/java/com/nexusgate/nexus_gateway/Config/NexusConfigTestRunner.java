package com.nexusgate.nexus_gateway.Config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class NexusConfigTestRunner implements CommandLineRunner {

    private final NexusConfigLoader configLoader;

    public NexusConfigTestRunner(NexusConfigLoader configLoader) {
        this.configLoader = configLoader;
    }

    @Override
    public void run(String... args) {

        NexusConfig config = configLoader.load();

       System.out.println("Global management endpoints: " + config.getManagement().getEndpoints());

       config.getServices().forEach((name, service) -> {

            System.out.println(
                    name + " -> " +
                            service.getUrl() + " -> " +
                            service.getPath()
            );

            if (service.getManagement() != null) {
                System.out.println(
                        "  Override: " +
                                service.getManagement().getEndpoints()
                );
            }
       });
    }
}