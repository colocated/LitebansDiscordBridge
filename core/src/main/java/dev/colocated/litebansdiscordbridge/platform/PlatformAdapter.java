package dev.colocated.litebansdiscordbridge.platform;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

/**
 * Abstracts the three platform-specific concerns this plugin needs:
 * online-player lookup, the data directory, and logging.
 *
 * Each platform module (bukkit / velocity / bungeecord) provides one implementation.
 */
public interface PlatformAdapter {

    /**
     * Returns the display name of an online player, or empty if offline.
     * Used to resolve player names from UUIDs for placeholder substitution.
     */
    Optional<String> getOnlinePlayerName(UUID uuid);

    /**
     * Directory where config.yml is stored (created on first run if absent).
     */
    Path getDataDirectory();

    void logInfo(String message);

    void logWarn(String message);

    void logError(String message, Throwable throwable);

    default void logError(String message) {
        logError(message, null);
    }
}
