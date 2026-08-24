package com.nexusgate.nexus_gateway.Config;

import lombok.Data;

import java.util.Map;

@Data
public class NexusConfig {

    private ManagementConfig management;
    private Map<String, ServiceConfig> services;
}