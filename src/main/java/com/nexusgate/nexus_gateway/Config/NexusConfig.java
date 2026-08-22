package com.nexusgate.nexus_gateway.Config;

import lombok.Data;

import java.util.Map;

@Data
public class NexusConfig {

    private Map<String, ServiceConfig> services;

    public Map<String, ServiceConfig> getServices() {
        return services;
    }

    public void setServices(Map<String, ServiceConfig> services) {
        this.services = services;
    }
}