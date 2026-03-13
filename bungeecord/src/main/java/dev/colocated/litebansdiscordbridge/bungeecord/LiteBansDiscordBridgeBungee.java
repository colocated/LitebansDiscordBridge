package dev.colocated.litebansdiscordbridge.bungeecord;

import dev.colocated.litebansdiscordbridge.config.ConfigManager;
import dev.colocated.litebansdiscordbridge.discord.DiscordWebhookSender;
import dev.colocated.litebansdiscordbridge.listener.LiteBansListener;
import litebans.api.Events;
import net.md_5.bungee.api.plugin.Plugin;

public class LiteBansDiscordBridgeBungee extends Plugin {

    private BungeePlatformAdapter platform;
    private ConfigManager configManager;
    private DiscordWebhookSender webhookSender;
    private LiteBansListener liteBansListener;

    @Override
    public void onEnable() {
        getLogger().info("Initializing LiteBansDiscordBridge...");

        try {
            platform = new BungeePlatformAdapter(this);

            configManager = new ConfigManager(platform.getDataDirectory());
            configManager.load();

            webhookSender = new DiscordWebhookSender(platform);

            liteBansListener = new LiteBansListener(configManager, webhookSender, platform);
            Events.get().register(liteBansListener);

            getProxy().getPluginManager().registerCommand(this, new BungeeCommand(configManager, platform));

            getLogger().info("LiteBansDiscordBridge enabled successfully!");
        } catch (Exception e) {
            getLogger().severe("Failed to initialize LiteBansDiscordBridge: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("Shutting down LiteBansDiscordBridge...");

        if (liteBansListener != null) {
            Events.get().unregister(liteBansListener);
        }

        if (webhookSender != null) {
            webhookSender.shutdown();
        }
    }
}
