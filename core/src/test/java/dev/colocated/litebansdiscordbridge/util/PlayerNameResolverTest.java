package dev.colocated.litebansdiscordbridge.util;

import dev.colocated.litebansdiscordbridge.support.TestDatabase;
import dev.colocated.litebansdiscordbridge.support.TestPlatform;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerNameResolverTest {

    private static final UUID OFFLINE = UUID.fromString("da56db05-3ca2-4614-8f83-1c6648958007");
    private static final UUID ONLINE = UUID.fromString("94660722-20ea-40eb-b508-7cd4abd6fd23");
    private static final UUID NEVER_SEEN = UUID.fromString("11111111-2222-3333-4444-555555555555");

    private TestDatabase database;
    private TestPlatform platform;

    @BeforeEach
    void setUp() {
        PlayerNameResolver.clearCache();
        database = TestDatabase.install().withName(OFFLINE, "Rewaked_");
        platform = new TestPlatform();
    }

    @AfterEach
    void tearDown() {
        PlayerNameResolver.clearCache();
        TestDatabase.uninstall();
    }

    @Test
    @DisplayName("an offline player resolves through the LiteBans name history")
    void resolvesOfflinePlayerFromLiteBans() {
        assertEquals("Rewaked_", PlayerNameResolver.resolve(OFFLINE, platform));
    }

    @Test
    @DisplayName("an online player resolves without querying the database")
    void prefersOnlinePlayer() {
        platform.online(ONLINE, "zxqld");

        assertEquals("zxqld", PlayerNameResolver.resolve(ONLINE, platform));
        assertEquals(0, database.lookups);
    }

    @Test
    @DisplayName("repeated lookups for the same player hit the database once")
    void cachesResolvedNames() {
        for (int i = 0; i < 20; i++) {
            assertEquals("Rewaked_", PlayerNameResolver.resolve(OFFLINE, platform));
        }

        assertEquals(1, database.lookups);
    }

    @Test
    @DisplayName("a player LiteBans has never seen falls back to the UUID")
    void fallsBackToUuidForUnknownPlayer() {
        assertEquals(NEVER_SEEN.toString(), PlayerNameResolver.resolve(NEVER_SEEN, platform));
    }

    @Test
    @DisplayName("a failing database falls back to the UUID and warns rather than propagating")
    void survivesDatabaseFailure() {
        TestDatabase.install().failing();

        assertEquals(OFFLINE.toString(), PlayerNameResolver.resolve(OFFLINE, platform));
        assertEquals(1, platform.warnings.size());
        assertTrue(platform.warnings.get(0).contains("database unreachable"), platform.warnings.toString());
    }

    @Test
    @DisplayName("LiteBans not being loaded falls back to the UUID")
    void survivesMissingLiteBans() {
        TestDatabase.uninstall();

        assertEquals(OFFLINE.toString(), PlayerNameResolver.resolve(OFFLINE, platform));
    }

    @Test
    @DisplayName("a null UUID never reaches the database")
    void handlesNullUuid() {
        assertEquals("Unknown", PlayerNameResolver.resolve(null, platform));
        assertEquals(0, database.lookups);
    }

    @Test
    @DisplayName("clearCache forces the next lookup to go back to the database")
    void clearCacheForcesRelookup() {
        PlayerNameResolver.resolve(OFFLINE, platform);
        PlayerNameResolver.clearCache();
        PlayerNameResolver.resolve(OFFLINE, platform);

        assertEquals(2, database.lookups);
    }

    @Test
    @DisplayName("a name learned while offline is superseded once the player comes online")
    void onlineNameWinsOverCachedName() {
        assertEquals("Rewaked_", PlayerNameResolver.resolve(OFFLINE, platform));

        platform.online(OFFLINE, "RenamedPlayer");

        assertEquals("RenamedPlayer", PlayerNameResolver.resolve(OFFLINE, platform));
        assertFalse(platform.warnings.size() > 0, platform.warnings.toString());
    }
}
