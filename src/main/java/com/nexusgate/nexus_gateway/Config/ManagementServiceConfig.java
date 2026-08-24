package com.nexusgate.nexus_gateway.Config;

import lombok.Data;

import java.util.List;

@Data
public class ManagementServiceConfig {

    private List<String> endpoints;
}