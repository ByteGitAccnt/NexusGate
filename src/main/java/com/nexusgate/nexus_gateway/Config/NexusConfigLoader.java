package com.nexusgate.nexus_gateway.Config;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class NexusConfigLoader {

    private final Path configPath = Path.of("nexus.yml");

    public NexusConfig load() {
        try (InputStream inputStream = Files.newInputStream(configPath)) {

            Yaml yaml = new Yaml();

            return yaml.loadAs(inputStream, NexusConfig.class);

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to load NexusGate configuration: " + configPath,
                    e
            );
        }
    }
}