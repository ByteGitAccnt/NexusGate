package com.nexusgate.nexus_gateway.Config;

import com.nexusgate.nexus_gateway.Config.management.ManagementServiceConfig;
import lombok.Data;

@Data
public class ServiceConfig {

    private String url;
    private String path;
    private ManagementServiceConfig management;

}