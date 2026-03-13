package dev.colocated.litebansdiscordbridge.config;

import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {

    private final Path dataDirectory;
    private final Path configPath;
    private YamlConfigurationLoader loader;
    private CommentedConfigurationNode rootNode;

    public ConfigManager(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.configPath = dataDirectory.resolve("config.yml");
    }

    public void load() throws IOException {
        if (!Files.exists(dataDirectory)) {
            Files.createDirectories(dataDirectory);
        }

        if (!Files.exists(configPath)) {
            createDefaultConfig();
        }

        loader = YamlConfigurationLoader.builder()
            .path(configPath)
            .build();

        rootNode = loader.load();
    }

    public void reload() throws IOException {
        load();
    }

    private void createDefaultConfig() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/config.yml")) {
            if (in != null) {
                Files.copy(in, configPath);
            } else {
                Files.createFile(configPath);
            }
        }
    }

    public CommentedConfigurationNode getNode() {
        return rootNode;
    }

    public CommentedConfigurationNode getNode(String path) {
        return rootNode.node((Object[]) path.split("\\."));
    }

    public String getString(String path, String defaultValue) {
        return rootNode.node((Object[]) path.split("\\.")).getString(defaultValue);
    }

    public boolean getBoolean(String path, boolean defaultValue) {
        return rootNode.node((Object[]) path.split("\\.")).getBoolean(defaultValue);
    }

    public int getInt(String path, int defaultValue) {
        return rootNode.node((Object[]) path.split("\\.")).getInt(defaultValue);
    }
}
