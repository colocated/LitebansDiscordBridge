package dev.colocated.litebansdiscordbridge.support;

import dev.colocated.litebansdiscordbridge.platform.PlatformAdapter;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** A {@link PlatformAdapter} with a controllable set of online players and captured log output. */
public final class TestPlatform implements PlatformAdapter {

    private final Map<UUID, String> onlinePlayers = new HashMap<UUID, String>();
    public final List<String> warnings = new ArrayList<String>();
    public final List<String> errors = new ArrayList<String>();

    public TestPlatform online(UUID uuid, String name) {
        onlinePlayers.put(uuid, name);
        return this;
    }

    @Override
    public Optional<String> getOnlinePlayerName(UUID uuid) {
        String name = onlinePlayers.get(uuid);
        return name != null ? Optional.of(name) : Optional.<String>empty();
    }

    @Override
    public Path getDataDirectory() {
        return Paths.get(".");
    }

    @Override
    public void logInfo(String message) {
    }

    @Override
    public void logWarn(String message) {
        warnings.add(message);
    }

    @Override
    public void logError(String message, Throwable throwable) {
        errors.add(message);
    }
}
