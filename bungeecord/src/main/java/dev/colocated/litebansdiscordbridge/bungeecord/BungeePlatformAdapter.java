package dev.colocated.litebansdiscordbridge.bungeecord;

import dev.colocated.litebansdiscordbridge.platform.PlatformAdapter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

public class BungeePlatformAdapter implements PlatformAdapter {

    private final Plugin plugin;

    public BungeePlatformAdapter(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Optional<String> getOnlinePlayerName(UUID uuid) {
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(uuid);
        return player != null ? Optional.of(player.getName()) : Optional.<String>empty();
    }

    @Override
    public Path getDataDirectory() {
        return plugin.getDataFolder().toPath();
    }

    @Override
    public void logInfo(String message) {
        plugin.getLogger().info(message);
    }

    @Override
    public void logWarn(String message) {
        plugin.getLogger().warning(message);
    }

    @Override
    public void logError(String message, Throwable throwable) {
        if (throwable != null) {
            plugin.getLogger().severe(message + ": " + throwable.getMessage());
        } else {
            plugin.getLogger().severe(message);
        }
    }
}
