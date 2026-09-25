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
           System.out.println("Rate limit strategy ; " + config.getRateLimit().getRedisFailureStrategy());
           System.out.println("Rate limit strategy time out; " + config.getRateLimit().getRedisTimeoutMs());
          /* RateLimitPolicy inner = config.getRateLimit().getInner();
           RateLimitPolicy outer = config.getRateLimit().getOuter();
           System.out.println(
                   "Is rate limited is enables :" +
                   inner.isEnabled() +
                           "\nalgorthm : " +
                           inner.getAlgorithm() +
                           "\nRefil rate:" +
                           inner.getRefillRate() +
                           "\nCapacity:" +
                           inner.getCapacity() +
                           "\nrefile rate per second :" +
                           inner.getRefillPeriodSeconds()
           );
           System.out.println(
                   "Is rate limited is enables :" +
                           outer.isEnabled() +
                           "\nalgorthm : " +
                           outer.getAlgorithm() +
                           "\nRefil rate:" +
                           outer.getRefillRate() +
                           "\nCapacity:" +
                           outer.getCapacity() +
                           "\nrefile rate per second :" +
                           outer.getRefillPeriodSeconds()
           );*/
       });
    }
}
