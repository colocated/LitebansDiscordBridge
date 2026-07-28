package dev.colocated.litebansdiscordbridge.util;

import dev.colocated.litebansdiscordbridge.platform.PlatformAdapter;
import litebans.api.Database;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves a player UUID to a username.
 *
 * Lookup order: the online player on this platform, then the LiteBans name history
 * table (which also covers offline players), then the UUID itself as a last resort.
 *
 * Results are cached because a single embed runs the placeholder replacer once per
 * title / description / field, and every miss would otherwise be its own database query.
 */
public final class PlayerNameResolver {

    private static final long HIT_TTL_MILLIS = 10 * 60 * 1000L;
    private static final long MISS_TTL_MILLIS = 60 * 1000L;
    private static final int MAX_CACHE_SIZE = 1000;

    private static final Map<UUID, CachedName> CACHE = new ConcurrentHashMap<UUID, CachedName>();

    private PlayerNameResolver() {
    }

    /**
     * Returns the player's name, falling back to the UUID string if it cannot be resolved.
     * Called from the LiteBans event thread, so the database lookup never touches the main thread.
     */
    public static String resolve(UUID uuid, PlatformAdapter platform) {
        if (uuid == null) {
            return "Unknown";
        }

        if (platform != null) {
            Optional<String> onlineName = platform.getOnlinePlayerName(uuid);
            if (onlineName.isPresent()) {
                put(uuid, onlineName.get());
                return onlineName.get();
            }
        }

        CachedName cached = CACHE.get(uuid);
        if (cached != null && !cached.isExpired()) {
            return cached.name != null ? cached.name : uuid.toString();
        }

        String historicalName = lookupInLiteBans(uuid, platform);
        put(uuid, historicalName);
        return historicalName != null ? historicalName : uuid.toString();
    }

    /** Drops every cached name — used on config reload so stale names can be picked up again. */
    public static void clearCache() {
        CACHE.clear();
    }

    private static String lookupInLiteBans(UUID uuid, PlatformAdapter platform) {
        try {
            String name = Database.get().getPlayerName(uuid);
            return name != null && !name.isEmpty() ? name : null;
        } catch (Throwable t) {
            // LiteBans not loaded yet, or the database is unreachable — fall back to the UUID
            if (platform != null) {
                platform.logWarn("Could not look up name for " + uuid + " in LiteBans: " + t.getMessage());
            }
            return null;
        }
    }

    private static void put(UUID uuid, String name) {
        if (CACHE.size() >= MAX_CACHE_SIZE) {
            pruneExpired();
            if (CACHE.size() >= MAX_CACHE_SIZE) {
                CACHE.clear();
            }
        }
        CACHE.put(uuid, new CachedName(name));
    }

    private static void pruneExpired() {
        Iterator<Map.Entry<UUID, CachedName>> it = CACHE.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().isExpired()) {
                it.remove();
            }
        }
    }

    private static final class CachedName {
        private final String name;
        private final long expiresAt;

        CachedName(String name) {
            this.name = name;
            this.expiresAt = System.currentTimeMillis() + (name != null ? HIT_TTL_MILLIS : MISS_TTL_MILLIS);
        }

        boolean isExpired() {
            return System.currentTimeMillis() >= expiresAt;
        }
    }
}
