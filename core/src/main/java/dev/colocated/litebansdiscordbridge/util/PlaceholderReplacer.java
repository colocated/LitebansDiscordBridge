package dev.colocated.litebansdiscordbridge.util;

import dev.colocated.litebansdiscordbridge.platform.PlatformAdapter;
import litebans.api.Entry;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;
import java.util.logging.Logger;

public class PlaceholderReplacer {

    private static final Logger LOGGER = Logger.getLogger(PlaceholderReplacer.class.getName());
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final String CONSOLE_UUID = "f78a4d8dd51b4b3998a3230f2de0c670";

    static {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public static String replace(String text, Entry entry, PlatformAdapter platform) {
        if (text == null || entry == null) {
            return text;
        }

        // Only resolve player name if needed (potentially expensive on large servers)
        if (text.contains("%player%") || text.contains("%player_name%")) {
            String playerName = resolvePlayerName(entry.getUuid(), platform);
            text = text.replace("%player%", playerName)
                       .replace("%player_name%", playerName);
        }

        if (text.contains("%player_uuid%")) {
            String playerUuid = entry.getUuid() != null ? entry.getUuid() : "Unknown";
            text = text.replace("%player_uuid%", playerUuid);
        }

        if (text.contains("%executor_uuid%")) {
            String executorUuid = resolveExecutorUuid(entry.getExecutorUUID());
            text = text.replace("%executor_uuid%", executorUuid);
        }

        if (text.contains("%executor%") || text.contains("%executor_name%")) {
            String executorName = entry.getExecutorName() != null ? entry.getExecutorName() : "Console";
            text = text.replace("%executor%", executorName)
                       .replace("%executor_name%", executorName);
        }

        text = text
            .replace("%reason%", entry.getReason() != null ? entry.getReason() : "No reason specified")
            .replace("%type%", entry.getType() != null ? entry.getType() : "Unknown")
            .replace("%server%", entry.getServerOrigin() != null ? entry.getServerOrigin() : "Unknown")
            .replace("%server_origin%", entry.getServerOrigin() != null ? entry.getServerOrigin() : "Unknown")
            .replace("%server_scope%", entry.getServerScope() != null ? entry.getServerScope() : "Global")
            .replace("%ip%", entry.getIp() != null ? entry.getIp() : "Unknown")
            .replace("%ip_address%", entry.getIp() != null ? entry.getIp() : "Unknown")
            .replace("%id%", String.valueOf(entry.getId()))
            .replace("%id_random%", entry.getRandomID() != null ? entry.getRandomID() : String.valueOf(entry.getId()))
            .replace("%active%", String.valueOf(entry.isActive()))
            .replace("%permanent%", String.valueOf(entry.isPermanent()))
            .replace("%silent%", String.valueOf(entry.isSilent()))
            .replace("%ipban%", String.valueOf(entry.isIpban()))
            .replace("%removed_by_name%", String.valueOf(entry.getRemovedByName()))
            .replace("%removed_by_uuid%", String.valueOf(entry.getRemovedByUUID()))
            .replace("%removed_reason%", String.valueOf(entry.getRemovalReason()));

        if (text.contains("%duration%")) {
            text = text.replace("%duration%", formatDuration(entry.getRemainingDuration(System.currentTimeMillis())));
        }
        if (text.contains("%duration_original%")) {
            text = text.replace("%duration_original%", formatDuration(entry.getDuration()));
        }

        if (text.contains("%date%") || text.contains("%date_start%")) {
            String dateStart = formatDate(entry.getDateStart());
            text = text.replace("%date%", dateStart)
                       .replace("%date_start%", dateStart);
        }
        if (text.contains("%date_end%")) {
            text = text.replace("%date_end%", formatDate(entry.getDateEnd()));
        }

        return text;
    }

    private static String resolveExecutorUuid(String rawExecutor) {
        if (rawExecutor == null) {
            return CONSOLE_UUID;
        }
        try {
            String formatted = rawExecutor.replaceFirst(
                "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                "$1-$2-$3-$4-$5"
            );
            UUID uuid = UUID.fromString(formatted);
            return uuid.toString().replace("-", "");
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Invalid executor UUID format: " + rawExecutor + ", defaulting to console");
            return CONSOLE_UUID;
        }
    }

    private static String resolvePlayerName(String uuidString, PlatformAdapter platform) {
        if (uuidString == null || platform == null) {
            return "Unknown";
        }
        try {
            String formattedUuid = uuidString.replaceFirst(
                "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                "$1-$2-$3-$4-$5"
            );
            UUID uuid = UUID.fromString(formattedUuid);
            Optional<String> name = platform.getOnlinePlayerName(uuid);
            if (name.isPresent()) {
                return name.get();
            }
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Invalid player UUID format: " + uuidString);
            return "Unknown";
        }
        // Player offline — return raw UUID string
        return uuidString;
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
        return DATE_FORMAT.format(new Date(timestamp));
    }

    public static int parseColor(String colorString) {
        if (colorString == null || colorString.isEmpty()) return 0;
        try {
            if (colorString.startsWith("#")) {
                return Integer.parseInt(colorString.substring(1), 16);
            } else {
                return Integer.parseInt(colorString);
            }
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
