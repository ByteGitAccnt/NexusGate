/*
package com.nexusgate.nexus_gateway.Config.RateLimit;

import com.nexusgate.nexus_gateway.Config.NexusConfig;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/test/rate-limit")
public class RateLimitTestController {

    private final RedisTokenBucket tokenBucket;
    private final ClientIpResolver clientIpResolver;
    private final RateLimitKeyGenerator rateLimitKeyGenerator;
    private final NexusConfig nexusConfig;


    public RateLimitTestController(RedisTokenBucket tokenBucket, ClientIpResolver clientIpResolver, RateLimitKeyGenerator rateLimitKeyGenerator , NexusConfig nexusConfig ) {
        this.tokenBucket = tokenBucket;
        this.clientIpResolver = clientIpResolver;
        this.rateLimitKeyGenerator = rateLimitKeyGenerator;
        this.nexusConfig = nexusConfig;
    }

    @GetMapping
    public Mono<RateLimitResult> test() {

        RateLimitPolicy policy = new RateLimitPolicy();
        policy.setEnabled(true);
        policy.setAlgorithm("token-bucket");
        policy.setCapacity(5);
        policy.setRefillRate(2);
        policy.setRefillPeriodSeconds(1);

        return tokenBucket.consume(
                "nexusgate:ratelimit:test",
                policy
        );
    }

    @GetMapping("/ip")
    public String testIp(ServerWebExchange exchange) {
        return clientIpResolver.resolve(exchange);
    }

    @GetMapping("/key")
    public String testKey(ServerWebExchange exchange) {
        return rateLimitKeyGenerator.forIp(clientIpResolver.resolve(exchange));
    }

    @GetMapping("/outer")
    public Mono<RateLimitResult> testOuterRateLimit(ServerWebExchange exchange) {
        String ip = clientIpResolver.resolve(exchange);
        String key = rateLimitKeyGenerator.forIp(ip);
        RateLimitPolicy policy = nexusConfig.getRateLimit().getOuter();
        return tokenBucket.consume(key, policy);
    }

    @GetMapping("/identity")
    public Mono<String> testIdentity() {

        return ReactiveSecurityContextHolder.getContext()
                .map(context -> {
                    Authentication authentication =
                            context.getAuthentication();

                    return authentication.getName() != null
                            ? authentication.getName()
                            : "Unknown User";
                })
                .defaultIfEmpty("No Authentication");
    }

    @GetMapping("/user")
    public String testUser(ServerWebExchange exchange) {
        return rateLimitKeyGenerator.forUser(clientIpResolver.resolve(exchange));
    }
}*/
