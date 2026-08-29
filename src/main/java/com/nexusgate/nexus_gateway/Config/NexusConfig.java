package com.nexusgate.nexus_gateway.Config;

import com.nexusgate.nexus_gateway.Config.Security.SecurityConfig;
import com.nexusgate.nexus_gateway.Config.management.ManagementConfig;
import lombok.Data;

import java.util.Map;

@Data
public class NexusConfig {

    private ManagementConfig management;
    private Map<String, ServiceConfig> services;
    private SecurityConfig security;
}