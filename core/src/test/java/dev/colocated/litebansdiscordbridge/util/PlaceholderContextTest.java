package dev.colocated.litebansdiscordbridge.util;

import dev.colocated.litebansdiscordbridge.support.TestDatabase;
import dev.colocated.litebansdiscordbridge.support.TestEntry;
import dev.colocated.litebansdiscordbridge.support.TestPlatform;
import litebans.api.Entry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class PlaceholderContextTest {

    private static final UUID PLAYER = UUID.fromString(TestEntry.PLAYER_UUID);

    private TestDatabase database;
    private TestPlatform platform;

    @BeforeEach
    void setUp() {
        PlayerNameResolver.clearCache();
        database = TestDatabase.install().withName(PLAYER, "Rewaked_");
        platform = new TestPlatform();
    }

    @AfterEach
    void tearDown() {
        PlayerNameResolver.clearCache();
        TestDatabase.uninstall();
    }

    private PlaceholderContext contextFor(Entry entry) {
        return PlaceholderContext.of(entry, platform);
    }

    private PlaceholderContext defaultContext() {
        return contextFor(TestEntry.builder().build());
    }

    @Nested
    @DisplayName("player name resolution")
    class PlayerName {

        @Test
        @DisplayName("an offline player renders their name, not their UUID")
        void offlinePlayerRendersName() {
            assertEquals("Rewaked_ has been muted.",
                defaultContext().apply("%player% has been muted."));
        }

        @Test
        @DisplayName("the whole embed costs a single name lookup")
        void resolvesNameOncePerContext() {
            PlaceholderContext context = defaultContext();

            for (int i = 0; i < 15; i++) {
                assertEquals("Rewaked_", context.apply("%player%"));
                assertEquals("Rewaked_", context.apply("%player_name%"));
            }

            assertEquals(1, database.lookups);
        }

        @Test
        @DisplayName("a config without %player% never looks a name up")
        void resolvesNameLazily() {
            PlaceholderContext context = defaultContext();

            context.apply("Punishment #%id% on %server% — %reason%");

            assertEquals(0, database.lookups);
        }

        @Test
        @DisplayName("an unresolvable player falls back to the UUID")
        void unresolvablePlayerFallsBackToUuid() {
            String unknown = "11111111-2222-3333-4444-555555555555";

            assertEquals(unknown, contextFor(TestEntry.builder().uuid(unknown).build()).apply("%player%"));
        }

        @Test
        @DisplayName("a malformed UUID renders as Unknown instead of throwing")
        void malformedUuidIsNotFatal() {
            assertEquals("Unknown", contextFor(TestEntry.builder().uuid("not-a-uuid").build()).apply("%player%"));
        }
    }

    @Nested
    @DisplayName("substitution")
    class Substitution {

        @Test
        @DisplayName("unknown placeholders are left untouched")
        void leavesUnknownPlaceholdersAlone() {
            assertEquals("%not_a_placeholder% stays", defaultContext().apply("%not_a_placeholder% stays"));
        }

        @Test
        @DisplayName("text pulled in by a placeholder is not itself expanded")
        void doesNotReexpandSubstitutedText() {
            Entry entry = TestEntry.builder().reason("saying %player_uuid% in chat").build();

            assertEquals("saying %player_uuid% in chat / " + TestEntry.PLAYER_UUID,
                contextFor(entry).apply("%reason% / %player_uuid%"));
        }

        @Test
        @DisplayName("a replacement containing $ or \\ is inserted literally")
        void handlesRegexReplacementMetacharacters() {
            Entry entry = TestEntry.builder().reason("cost $5 \\ nothing").build();

            assertEquals("cost $5 \\ nothing", contextFor(entry).apply("%reason%"));
        }

        @Test
        @DisplayName("null and placeholder-free templates pass straight through")
        void passesThroughTemplatesWithoutPlaceholders() {
            PlaceholderContext context = defaultContext();

            assertNull(context.apply(null));
            assertEquals("", context.apply(""));
            assertEquals("no placeholders here", context.apply("no placeholders here"));
        }

        @Test
        @DisplayName("placeholder names are case-insensitive")
        void isCaseInsensitive() {
            assertEquals("Rewaked_", defaultContext().apply("%PLAYER%"));
        }

        @Test
        @DisplayName("value() exposes a single placeholder without templating")
        void exposesIndividualValues() {
            PlaceholderContext context = defaultContext();

            assertEquals("rtp", context.value("server"));
            assertNull(context.value("nonexistent"));
        }
    }

    @Nested
    @DisplayName("entry fields")
    class Fields {

        @Test
        @DisplayName("each entry field maps to its placeholder")
        void mapsEntryFields() {
            Entry entry = TestEntry.builder()
                .id(1045L)
                .type("ban")
                .ip("10.0.0.7")
                .reason("cheating")
                .executorName("zxqld")
                .serverOrigin("rtp")
                .serverScope("global")
                .randomId("4AC6DA")
                .silent(true)
                .ipban(true)
                .active(true)
                .build();

            assertEquals("1045|ban|10.0.0.7|cheating|zxqld|rtp|global|4AC6DA|true|true|true",
                contextFor(entry).apply(
                    "%id%|%type%|%ip%|%reason%|%executor%|%server%|%server_scope%|%id_random%|%silent%|%ipban%|%active%"));
        }

        @Test
        @DisplayName("aliases resolve to the same value as their primary placeholder")
        void aliasesMatchPrimaries() {
            PlaceholderContext context = defaultContext();

            assertEquals(context.apply("%player%"), context.apply("%player_name%"));
            assertEquals(context.apply("%executor%"), context.apply("%executor_name%"));
            assertEquals(context.apply("%ip%"), context.apply("%ip_address%"));
            assertEquals(context.apply("%server%"), context.apply("%server_origin%"));
            assertEquals(context.apply("%date%"), context.apply("%date_start%"));
        }

        @Test
        @DisplayName("missing optional fields render a readable fallback, never the string null")
        void missingFieldsFallBackReadably() {
            Entry entry = TestEntry.builder()
                .reason(null)
                .executorName(null)
                .removedBy(null, null)
                .removalReason(null)
                .serverOrigin(null)
                .build();

            assertEquals("No reason specified|Console|Unknown|Unknown|No reason specified|Unknown",
                contextFor(entry).apply(
                    "%reason%|%executor%|%removed_by_name%|%removed_by_uuid%|%removed_reason%|%server%"));
        }

        @Test
        @DisplayName("an unban records who lifted it")
        void rendersRemovalDetails() {
            Entry entry = TestEntry.builder()
                .removedBy("94660722-20ea-40eb-b508-7cd4abd6fd23", "zxqld")
                .removalReason("appealed")
                .build();

            assertEquals("zxqld|appealed", contextFor(entry).apply("%removed_by_name%|%removed_reason%"));
        }

        @Test
        @DisplayName("a console executor gets the console UUID, and a real one is stripped of dashes")
        void resolvesExecutorUuid() {
            assertEquals("f78a4d8dd51b4b3998a3230f2de0c670",
                contextFor(TestEntry.builder().executorUuid(null).build()).apply("%executor_uuid%"));

            assertEquals("9466072220ea40ebb5087cd4abd6fd23",
                contextFor(TestEntry.builder()
                    .executorUuid("94660722-20ea-40eb-b508-7cd4abd6fd23").build()).apply("%executor_uuid%"));

            assertEquals("f78a4d8dd51b4b3998a3230f2de0c670",
                contextFor(TestEntry.builder().executorUuid("garbage").build()).apply("%executor_uuid%"));
        }

        @Test
        @DisplayName("durations render in the largest whole unit, zero meaning permanent")
        void formatsDurations() {
            assertEquals("Permanent", contextFor(TestEntry.builder()
                .remainingDuration(0L).build()).apply("%duration%"));
            assertEquals("45 seconds", contextFor(TestEntry.builder()
                .remainingDuration(45_000L).build()).apply("%duration%"));
            assertEquals("1 minute", contextFor(TestEntry.builder()
                .remainingDuration(60_000L).build()).apply("%duration%"));
            assertEquals("3 hours", contextFor(TestEntry.builder()
                .remainingDuration(3 * 3_600_000L).build()).apply("%duration%"));
            assertEquals("2 days", contextFor(TestEntry.builder()
                .remainingDuration(2 * 86_400_000L).build()).apply("%duration%"));
        }

        @Test
        @DisplayName("the original duration is reported separately from the remaining one")
        void formatsOriginalDuration() {
            Entry entry = TestEntry.builder().duration(7 * 86_400_000L).remainingDuration(3_600_000L).build();

            assertEquals("1 hour|7 days", contextFor(entry).apply("%duration%|%duration_original%"));
        }

        @Test
        @DisplayName("dates render in UTC, and an absent expiry reads as Never")
        void formatsDates() {
            Entry entry = TestEntry.builder().dateStart(1_700_000_000_000L).dateEnd(0L).build();

            assertEquals("2023-11-14 22:13:20|Never", contextFor(entry).apply("%date_start%|%date_end%"));
        }
    }

    @Test
    @DisplayName("every documented placeholder resolves to a value")
    void resolvesEveryKnownPlaceholder() {
        PlaceholderContext context = defaultContext();

        for (String placeholder : PlaceholderContext.KNOWN_PLACEHOLDERS) {
            assertNotNull(context.value(placeholder), "unresolved placeholder: %" + placeholder + "%");
            assertEquals(context.value(placeholder), context.apply("%" + placeholder + "%"),
                "apply() and value() disagree for %" + placeholder + "%");
        }
    }
}
