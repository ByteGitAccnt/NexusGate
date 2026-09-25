/*
package com.nexusgate.nexus_gateway.Config;

import com.nexusgate.nexus_gateway.Config.RateLimit.RateLimitPolicy;
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

       });
    }
}
*/
