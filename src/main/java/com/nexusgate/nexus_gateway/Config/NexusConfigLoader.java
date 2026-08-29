package com.nexusgate.nexus_gateway.Config;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class NexusConfigLoader {

    private final Path configPath = Path.of("nexus.yml");
    private final EnvironmentVariableResolver environmentResolver;

    public NexusConfigLoader(EnvironmentVariableResolver environmentResolver) {
        this.environmentResolver = environmentResolver;
    }

    public NexusConfig load() {
        try {
            String yamlContent = Files.readString(configPath);
            String resolvedYaml = environmentResolver.resolve(yamlContent);
            Yaml yaml = new Yaml();
            NexusConfig config = yaml.loadAs(resolvedYaml, NexusConfig.class);
            System.out.println(
                    "JWT secret resolved: " +
                            !config.getSecurity()
                                    .getJwt()
                                    .getVerification()
                                    .getSecret()
                                    .equals("${JWT_SECRET}")
            );
            return config;

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to load NexusGate configuration: "
                            + configPath,
                    e
            );
        }
    }
}