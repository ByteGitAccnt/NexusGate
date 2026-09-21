package com.nexusgate.nexus_gateway.Filter;

import com.nexusgate.nexus_gateway.Config.NexusConfig;
import com.nexusgate.nexus_gateway.Config.RateLimit.*;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class OuterRateLimitFilter implements GlobalFilter , Ordered {

    private final ClientIpResolver clientIpResolver;
    private final RateLimitKeyGenerator KeyGenerator;
    private final RedisTokenBucket tokenBucket;
    private final NexusConfig nexusConfig;
    //TODO :ENCHANTMENT AFTER COMPLETION:  we need to add a header check for proxies which are listed in the config file,
    // if the request is coming from a proxy which is not listed in the config file, we will reject the request
    // if no proxies listed and strategy is 'remote-address' then we allow by ips
    public OuterRateLimitFilter(ClientIpResolver clientIpResolver, RateLimitKeyGenerator rateLimitKeyGenerator, RedisTokenBucket tokenBucket, NexusConfig nexusConfig) {
        this.clientIpResolver = clientIpResolver;
        this.KeyGenerator = rateLimitKeyGenerator;
        this.tokenBucket = tokenBucket;
        this.nexusConfig = nexusConfig;
    }

    @Override
    public Mono<Void> filter( ServerWebExchange exchange,GatewayFilterChain chain) {
        RateLimitPolicy policy = nexusConfig.getRateLimit().getOuter();
        if(!policy.isEnabled()){
            return chain.filter(exchange);
        }
        String ip = clientIpResolver.resolve(exchange);
        String bucketKey = KeyGenerator.forIp(ip);
        return tokenBucket.consume(bucketKey , policy).flatMap(result -> handleResult(exchange, chain, result, policy));
    }

    private Mono<Void> handleResult(ServerWebExchange exchange, GatewayFilterChain chain , RateLimitResult result , RateLimitPolicy policy) {
        exchange.getResponse()
                .getHeaders()
                .add("X-RateLimit-Limit", String.valueOf(policy.getCapacity()));
        exchange.getResponse()
                .getHeaders()
                .add("X-RateLimit-Remaining", String.valueOf(result.remaining()));
        if(!result.allowed()){
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse()
                    .getHeaders()
                    .add("Retry-After", String.valueOf(result.retryAfter()));
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange);
    }
    @Override
    public int getOrder() {
        return -100;
    }
}
