package dev.colocated.litebansdiscordbridge.velocity;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.colocated.litebansdiscordbridge.platform.PlatformAdapter;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

public class VelocityPlatformAdapter implements PlatformAdapter {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    public VelocityPlatformAdapter(ProxyServer server, Logger logger, Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Override
    public Optional<String> getOnlinePlayerName(UUID uuid) {
        Optional<Player> player = server.getPlayer(uuid);
        if (player.isPresent()) {
            return Optional.of(player.get().getUsername());
        }
        return Optional.empty();
    }

    @Override
    public Path getDataDirectory() {
        return dataDirectory;
    }

    @Override
    public void logInfo(String message) {
        logger.info(message);
    }

    @Override
    public void logWarn(String message) {
        logger.warn(message);
    }

    @Override
    public void logError(String message, Throwable throwable) {
        if (throwable != null) {
            logger.error(message, throwable);
        } else {
            logger.error(message);
        }
    }
}
