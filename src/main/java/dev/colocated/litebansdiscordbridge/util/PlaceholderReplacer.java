package dev.colocated.litebansdiscordbridge.util;

import com.velocitypowered.api.proxy.ProxyServer;

import litebans.api.Entry;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlaceholderReplacer {
    private static final Logger logger = LoggerFactory.getLogger(PlaceholderReplacer.class);
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final String CONSOLE_UUID = "f78a4d8dd51b4b3998a3230f2de0c670";

    static {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public static String replace(String text, Entry entry, ProxyServer server) {
        if (text == null || entry == null) {
            return text;
        }

        // Resolve player name from UUID
        String playerName = resolvePlayerName(entry.getUuid(), server);
        String playerUuid = entry.getUuid() != null ? entry.getUuid() : "Unknown";

        // Resolve executor UUID - use helper to return UUID without dashes or console UUID
        String executorUuid = resolveExecutorUuid(entry.getExecutorUUID());

        return text
            .replace("%player%", playerName)
            .replace("%player_name%", playerName)
            .replace("%player_uuid%", playerUuid)
            .replace("%executor%", entry.getExecutorName() != null ? entry.getExecutorName() : "Console")
            .replace("%executor_name%", entry.getExecutorName() != null ? entry.getExecutorName() : "Console")
            .replace("%executor_uuid%", executorUuid)
            .replace("%reason%", entry.getReason() != null ? entry.getReason() : "No reason specified")
            .replace("%type%", entry.getType() != null ? entry.getType() : "Unknown")
            .replace("%duration%", formatDuration(entry.getRemainingDuration(System.currentTimeMillis())))
            .replace("%duration_original%", formatDuration(entry.getDuration()))
            .replace("%server%", entry.getServerOrigin() != null ? entry.getServerOrigin() : "Unknown")
            .replace("%server_origin%", entry.getServerOrigin() != null ? entry.getServerOrigin() : "Unknown")
            .replace("%server_scope%", entry.getServerScope() != null ? entry.getServerScope() : "Global")
            .replace("%date%", formatDate(entry.getDateStart()))
            .replace("%date_start%", formatDate(entry.getDateStart()))
            .replace("%date_end%", formatDate(entry.getDateEnd()))
            .replace("%ip%", entry.getIp() != null ? entry.getIp() : "Unknown")
            .replace("%ip_address%", entry.getIp() != null ? entry.getIp() : "Unknown")
            .replace("%id%", String.valueOf(entry.getId()))
            .replace("%active%", String.valueOf(entry.isActive()))
            .replace("%permanent%", String.valueOf(entry.isPermanent()))
            .replace("%silent%", String.valueOf(entry.isSilent()))
            .replace("%ipban%", String.valueOf(entry.isIpban()));
    }

    /**
     * Resolves an executor UUID string to a no-dash UUID.
     * If the provided string cannot be parsed as a UUID, returns the console UUID constant.
     * Accepts UUIDs with or without dashes.
     */
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
            logger.warn("Invalid executor UUID format: {}, defaulting to console...", rawExecutor);
            return CONSOLE_UUID;
        }
    }

    /**
     * Resolves a player name from their UUID.
     * Checks if the player is online first, otherwise returns the UUID.
     */
    private static String resolvePlayerName(String uuidString, ProxyServer server) {
        if (uuidString == null || server == null) {
            return "Unknown";
        }

        try {
            // Parse UUID (handle both with and without hyphens)
            String formattedUuid = uuidString.replaceFirst(
                "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                "$1-$2-$3-$4-$5"
            );
            UUID uuid = UUID.fromString(formattedUuid);

            // Check if player is online
            Optional<com.velocitypowered.api.proxy.Player> player = server.getPlayer(uuid);
            if (player.isPresent()) {
                return player.get().getUsername();
            }
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid UUID format: {}", uuidString);
            return "Unknown";
        }

        // Player is offline
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

        if (days > 0) {
            return days + " day" + (days > 1 ? "s" : "");
        } else if (hours > 0) {
            return hours + " hour" + (hours > 1 ? "s" : "");
        } else if (minutes > 0) {
            return minutes + " minute" + (minutes > 1 ? "s" : "");
        } else {
            return seconds + " second" + (seconds > 1 ? "s" : "");
        }
    }

    private static String formatDate(long timestamp) {
        if (timestamp <= 0) {
            return "Never";
        }
        return DATE_FORMAT.format(new Date(timestamp));
    }

    public static int parseColor(String colorString) {
        if (colorString == null || colorString.isEmpty()) {
            return 0;
        }

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
