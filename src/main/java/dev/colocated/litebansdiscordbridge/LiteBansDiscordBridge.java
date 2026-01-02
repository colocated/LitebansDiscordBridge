package dev.colocated.litebansdiscordbridge;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.colocated.litebansdiscordbridge.command.LiteBansDiscordCommand;
import dev.colocated.litebansdiscordbridge.config.ConfigManager;
import dev.colocated.litebansdiscordbridge.discord.DiscordWebhookSender;
import dev.colocated.litebansdiscordbridge.listener.LiteBansListener;
import litebans.api.Events;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(
    id = "litebansdiscordbridge",
    name = "LiteBansDiscordBridge",
    version = "1.0.0",
    description = "Sends LiteBans events to Discord webhooks",
    authors = {"colocated"}
)
public class LiteBansDiscordBridge {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    private ConfigManager configManager;
    private DiscordWebhookSender webhookSender;
    private LiteBansListener liteBansListener;

    @Inject
    public LiteBansDiscordBridge(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("Initializing LiteBansDiscordBridge...");

        try {
            configManager = new ConfigManager(this);
            configManager.load();

            webhookSender = new DiscordWebhookSender(this);

            liteBansListener = new LiteBansListener(this);
            Events.get().register(liteBansListener);

            // Register command
            CommandManager commandManager = server.getCommandManager();
            CommandMeta commandMeta = commandManager.metaBuilder("litebansdiscord")
                .aliases("ldb", "litebansbridge")
                .build();
            commandManager.register(commandMeta, new LiteBansDiscordCommand(this));

            logger.info("LiteBansDiscordBridge enabled successfully!");
        } catch (Exception e) {
            logger.error("Failed to initialize LiteBansDiscordBridge", e);
        }
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        logger.info("Shutting down LiteBansDiscordBridge...");

        if (liteBansListener != null) {
            Events.get().unregister(liteBansListener);
        }

        if (webhookSender != null) {
            webhookSender.shutdown();
        }
    }

    public ProxyServer getServer() {
        return server;
    }

    public Logger getLogger() {
        return logger;
    }

    public Path getDataDirectory() {
        return dataDirectory;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DiscordWebhookSender getWebhookSender() {
        return webhookSender;
    }
}
