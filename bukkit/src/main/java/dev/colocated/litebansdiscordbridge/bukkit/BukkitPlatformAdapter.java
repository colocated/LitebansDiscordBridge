package dev.colocated.litebansdiscordbridge.bukkit;

import dev.colocated.litebansdiscordbridge.platform.PlatformAdapter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

public class BukkitPlatformAdapter implements PlatformAdapter {

    private final JavaPlugin plugin;

    public BukkitPlatformAdapter(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Optional<String> getOnlinePlayerName(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
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
