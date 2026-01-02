package dev.colocated.litebansdiscordbridge.config;

import dev.colocated.litebansdiscordbridge.LiteBansDiscordBridge;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {

    private final LiteBansDiscordBridge plugin;
    private final Path configPath;
    private YamlConfigurationLoader loader;
    private CommentedConfigurationNode rootNode;

    public ConfigManager(LiteBansDiscordBridge plugin) {
        this.plugin = plugin;
        this.configPath = plugin.getDataDirectory().resolve("config.yml");
    }

    public void load() throws IOException {
        if (!Files.exists(plugin.getDataDirectory())) {
            Files.createDirectories(plugin.getDataDirectory());
        }

        if (!Files.exists(configPath)) {
            createDefaultConfig();
        }

        loader = YamlConfigurationLoader.builder()
            .path(configPath)
            .build();

        rootNode = loader.load();
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

    public void reload() throws IOException {
        load();
    }

    public CommentedConfigurationNode getNode() {
        return rootNode;
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

    public CommentedConfigurationNode getNode(String path) {
        return rootNode.node((Object[]) path.split("\\."));
    }
}
