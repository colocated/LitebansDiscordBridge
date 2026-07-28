package dev.colocated.litebansdiscordbridge.util;

import dev.colocated.litebansdiscordbridge.platform.PlatformAdapter;
import litebans.api.Entry;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The placeholder values for a single punishment, resolved once and reused for every
 * part of the embed (title, description, each field, footer, ...).
 *
 * Build one per event with {@link #of(Entry, PlatformAdapter)} and call {@link #apply(String)}
 * for each template string. Everything except the player name is a plain field read and is
 * resolved up front; the player name is looked up lazily, so a config that never uses
 * %player% never touches the database.
 */
public final class PlaceholderContext {

    private static final Logger LOGGER = Logger.getLogger(PlaceholderContext.class.getName());

    /** Thread-safe, unlike SimpleDateFormat — LiteBans may fire events from more than one thread. */
    private static final DateTimeFormatter DATE_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

    private static final Pattern PLACEHOLDER = Pattern.compile("%([a-zA-Z0-9_]+)%");

    private static final String CONSOLE_UUID = "f78a4d8dd51b4b3998a3230f2de0c670";

    /** Resolved lazily because the lookup can hit the LiteBans database. */
    private static final Set<String> PLAYER_NAME_KEYS =
        Collections.unmodifiableSet(new HashSet<String>(Arrays.asList("player", "player_name")));

    /** Every placeholder the plugin understands, for config validation and documentation. */
    public static final Set<String> KNOWN_PLACEHOLDERS = Collections.unmodifiableSet(
        new HashSet<String>(Arrays.asList(
            "player", "player_name", "player_uuid",
            "executor", "executor_name", "executor_uuid",
            "reason", "type",
            "server", "server_origin", "server_scope",
            "ip", "ip_address",
            "id", "id_random",
            "active", "permanent", "silent", "ipban",
            "removed_by_name", "removed_by_uuid", "removed_reason",
            "duration", "duration_original",
            "date", "date_start", "date_end")));

    private final Map<String, String> values;
    private final String playerUuid;
    private final PlatformAdapter platform;
    private String playerName;

    private PlaceholderContext(Entry entry, PlatformAdapter platform) {
        this.platform = platform;
        this.playerUuid = entry.getUuid();
        this.values = new HashMap<String, String>();

        long now = System.currentTimeMillis();

        put("player_uuid", entry.getUuid(), "Unknown");
        put("executor_uuid", resolveExecutorUuid(entry.getExecutorUUID()), CONSOLE_UUID);
        put("executor", entry.getExecutorName(), "Console");
        put("executor_name", entry.getExecutorName(), "Console");
        put("reason", entry.getReason(), "No reason specified");
        put("type", entry.getType(), "Unknown");
        put("server", entry.getServerOrigin(), "Unknown");
        put("server_origin", entry.getServerOrigin(), "Unknown");
        put("server_scope", entry.getServerScope(), "Global");
        put("ip", entry.getIp(), "Unknown");
        put("ip_address", entry.getIp(), "Unknown");
        put("id", String.valueOf(entry.getId()), "Unknown");
        put("id_random", entry.getRandomID(), String.valueOf(entry.getId()));
        put("active", String.valueOf(entry.isActive()), "false");
        put("permanent", String.valueOf(entry.isPermanent()), "false");
        put("silent", String.valueOf(entry.isSilent()), "false");
        put("ipban", String.valueOf(entry.isIpban()), "false");
        put("removed_by_name", entry.getRemovedByName(), "Unknown");
        put("removed_by_uuid", entry.getRemovedByUUID(), "Unknown");
        put("removed_reason", entry.getRemovalReason(), "No reason specified");
        put("duration", formatDuration(entry.getRemainingDuration(now)), "Permanent");
        put("duration_original", formatDuration(entry.getDuration()), "Permanent");
        put("date", formatDate(entry.getDateStart()), "Never");
        put("date_start", formatDate(entry.getDateStart()), "Never");
        put("date_end", formatDate(entry.getDateEnd()), "Never");
    }

    public static PlaceholderContext of(Entry entry, PlatformAdapter platform) {
        return new PlaceholderContext(entry, platform);
    }

    /**
     * Substitutes every known placeholder in one pass. Unknown placeholders are left
     * untouched, and text pulled in by a placeholder (a reason, a player name) is never
     * itself scanned for placeholders.
     */
    public String apply(String template) {
        if (template == null || template.indexOf('%') < 0) {
            return template;
        }

        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuffer out = new StringBuffer(template.length());
        while (matcher.find()) {
            String replacement = value(matcher.group(1));
            matcher.appendReplacement(out,
                Matcher.quoteReplacement(replacement != null ? replacement : matcher.group()));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    /** The resolved value for a placeholder name (without the % markers), or null if unknown. */
    public String value(String placeholder) {
        String key = placeholder.toLowerCase();
        return PLAYER_NAME_KEYS.contains(key) ? playerName() : values.get(key);
    }

    private String playerName() {
        if (playerName == null) {
            playerName = resolvePlayerName(playerUuid, platform);
        }
        return playerName;
    }

    private void put(String key, String value, String fallback) {
        values.put(key, value != null && !value.isEmpty() ? value : fallback);
    }

    private static String resolvePlayerName(String uuidString, PlatformAdapter platform) {
        if (uuidString == null) {
            return "Unknown";
        }
        UUID uuid = parseUuid(uuidString);
        if (uuid == null) {
            LOGGER.warning("Invalid player UUID format: " + uuidString);
            return "Unknown";
        }
        return PlayerNameResolver.resolve(uuid, platform);
    }

    private static String resolveExecutorUuid(String rawExecutor) {
        if (rawExecutor == null) {
            return CONSOLE_UUID;
        }
        UUID uuid = parseUuid(rawExecutor);
        if (uuid == null) {
            LOGGER.warning("Invalid executor UUID format: " + rawExecutor + ", defaulting to console");
            return CONSOLE_UUID;
        }
        return uuid.toString().replace("-", "");
    }

    /** Accepts both dashed and undashed UUIDs, as LiteBans stores either depending on version. */
    static UUID parseUuid(String raw) {
        try {
            return UUID.fromString(raw.replaceFirst(
                "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                "$1-$2-$3-$4-$5"));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String formatDuration(long milliseconds) {
        if (milliseconds <= 0) {
            return "Permanent";
        }
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) return days + " day" + (days > 1 ? "s" : "");
        if (hours > 0) return hours + " hour" + (hours > 1 ? "s" : "");
        if (minutes > 0) return minutes + " minute" + (minutes > 1 ? "s" : "");
        return seconds + " second" + (seconds > 1 ? "s" : "");
    }

    private static String formatDate(long timestamp) {
        if (timestamp <= 0) return "Never";
        return DATE_FORMAT.format(Instant.ofEpochMilli(timestamp));
    }
}
