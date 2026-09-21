package com.nexusgate.nexus_gateway.Config.RateLimit;

import lombok.Data;

@Data
public class RateLimitConfig {

    private RateLimitPolicy outer;
    private RateLimitPolicy inner;
}
