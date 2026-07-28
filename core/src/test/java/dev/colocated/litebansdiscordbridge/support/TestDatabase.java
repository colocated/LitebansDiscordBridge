package dev.colocated.litebansdiscordbridge.support;

import litebans.api.Database;
import litebans.api.Entry;

import java.sql.PreparedStatement;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stand-in for the LiteBans database. Installed with {@link Database#setInstance(Database)},
 * which is how LiteBans itself wires up its implementation at runtime.
 */
public final class TestDatabase extends Database {

    private final Map<UUID, String> knownNames = new HashMap<UUID, String>();
    private boolean failing = false;

    /** Number of getPlayerName calls since construction — used to assert lookups are not repeated. */
    public int lookups = 0;

    public static TestDatabase install() {
        TestDatabase database = new TestDatabase();
        Database.setInstance(database);
        return database;
    }

    /** Removes the instance entirely, simulating LiteBans not being loaded. */
    public static void uninstall() {
        Database.setInstance(null);
    }

    public TestDatabase withName(UUID uuid, String name) {
        knownNames.put(uuid, name);
        return this;
    }

    /** Makes every lookup throw, simulating an unreachable database. */
    public TestDatabase failing() {
        this.failing = true;
        return this;
    }

    @Override
    public String getPlayerName(UUID uuid) {
        lookups++;
        if (failing) {
            throw new IllegalStateException("database unreachable");
        }
        return knownNames.get(uuid);
    }

    @Override public boolean isPlayerBanned(UUID uuid, String scope) { return false; }
    @Override public boolean isPlayerBanned(UUID uuid, String ip, String scope) { return false; }
    @Override public boolean isPlayerMuted(UUID uuid, String scope) { return false; }
    @Override public boolean isPlayerMuted(UUID uuid, String ip, String scope) { return false; }
    @Override public Entry getBan(long id, String scope) { return null; }
    @Override public Entry getBan(UUID uuid, String ip, String scope) { return null; }
    @Override public Entry getMute(long id, String scope) { return null; }
    @Override public Entry getMute(UUID uuid, String ip, String scope) { return null; }
    @Override public Entry getWarning(long id, String scope) { return null; }
    @Override public Entry getWarning(UUID uuid, String ip, String scope) { return null; }
    @Override public Entry getKick(UUID uuid, String ip, String scope) { return null; }
    @Override public Entry getKick(long id, String scope) { return null; }
    @Override public Collection<UUID> getUsersByIP(String ip) { return Collections.emptyList(); }
    @Override public PreparedStatement prepareStatement(String sql) { return null; }
}
