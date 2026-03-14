package dev.colocated.litebansdiscordbridge.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.colocated.litebansdiscordbridge.config.ConfigManager;
import dev.colocated.litebansdiscordbridge.discord.DiscordWebhookSender;
import dev.colocated.litebansdiscordbridge.listener.LiteBansListener;
import litebans.api.Events;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(
    id = "litebansdiscordbridge",
    name = "LiteBansDiscordBridge",
    version = "2.0.0",
    description = "Sends LiteBans events to Discord webhooks",
    authors = {"colocated"},
    dependencies = { @Dependency(id = "litebans") }
)
public class LiteBansDiscordBridgeVelocity {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    private VelocityPlatformAdapter platform;
    private ConfigManager configManager;
    private DiscordWebhookSender webhookSender;
    private LiteBansListener liteBansListener;

    @Inject
    public LiteBansDiscordBridgeVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("Initializing LiteBansDiscordBridge...");

        try {
            platform = new VelocityPlatformAdapter(server, logger, dataDirectory);

            configManager = new ConfigManager(dataDirectory);
            configManager.load();

            webhookSender = new DiscordWebhookSender(platform);

            liteBansListener = new LiteBansListener(configManager, webhookSender, platform);
            Events.get().register(liteBansListener);

            CommandManager commandManager = server.getCommandManager();
            CommandMeta commandMeta = commandManager.metaBuilder("litebansdiscord")
                .aliases("ldb", "litebansbridge")
                .build();
            commandManager.register(commandMeta, new VelocityCommand(configManager, platform));

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
}
