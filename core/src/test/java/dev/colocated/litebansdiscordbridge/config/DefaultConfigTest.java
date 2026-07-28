package dev.colocated.litebansdiscordbridge.config;

import dev.colocated.litebansdiscordbridge.util.PlaceholderContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.spongepowered.configurate.CommentedConfigurationNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the shipped config.yml — a typo there is invisible until it reaches a Discord embed. */
class DefaultConfigTest {

    private static final Pattern PLACEHOLDER = Pattern.compile("%([a-zA-Z0-9_]+)%");

    private static final String[] EVENT_TYPES = {"ban", "mute", "warn", "kick", "unban", "unmute"};

    @Test
    @DisplayName("every placeholder used in the default config is one the plugin resolves")
    void defaultConfigUsesOnlyKnownPlaceholders() throws IOException {
        Set<String> unknown = new LinkedHashSet<String>();

        Matcher matcher = PLACEHOLDER.matcher(readDefaultConfig());
        while (matcher.find()) {
            String placeholder = matcher.group(1).toLowerCase();
            if (!PlaceholderContext.KNOWN_PLACEHOLDERS.contains(placeholder)) {
                unknown.add(placeholder);
            }
        }

        assertTrue(unknown.isEmpty(), "config.yml references unknown placeholders: " + unknown);
    }

    @Test
    @DisplayName("every placeholder the plugin resolves is documented in the default config")
    void defaultConfigDocumentsEveryPlaceholder() throws IOException {
        String config = readDefaultConfig();
        Set<String> undocumented = new LinkedHashSet<String>();

        for (String placeholder : PlaceholderContext.KNOWN_PLACEHOLDERS) {
            if (!config.contains("%" + placeholder + "%")) {
                undocumented.add(placeholder);
            }
        }

        assertTrue(undocumented.isEmpty(), "placeholders missing from config.yml docs: " + undocumented);
    }

    @Test
    @DisplayName("the default config is written out and parsed on first run")
    void writesAndLoadsDefaultConfig(@TempDir Path dataDirectory) throws IOException {
        ConfigManager configManager = new ConfigManager(dataDirectory);

        configManager.load();

        assertTrue(Files.exists(dataDirectory.resolve("config.yml")), "config.yml was not created");
        assertTrue(configManager.getString("webhook-url", "").startsWith("https://"),
            "default webhook-url should be an https placeholder URL");

        for (String eventType : EVENT_TYPES) {
            CommentedConfigurationNode event = configManager.getNode("events." + eventType);
            assertFalse(event.virtual(), "missing event block: " + eventType);
            assertTrue(event.node("enabled").getBoolean(false), eventType + " should be enabled by default");
            assertFalse(event.node("embed").node("description").getString("").isEmpty(),
                eventType + " should have an embed description");
        }
    }

    @Test
    @DisplayName("an existing config is not overwritten on load")
    void keepsExistingConfig(@TempDir Path dataDirectory) throws IOException {
        Files.createDirectories(dataDirectory);
        Files.write(dataDirectory.resolve("config.yml"),
            "webhook-url: \"https://discord.com/api/webhooks/kept\"\n".getBytes("UTF-8"));

        ConfigManager configManager = new ConfigManager(dataDirectory);
        configManager.load();

        assertEquals("https://discord.com/api/webhooks/kept", configManager.getString("webhook-url", ""));
    }

    private String readDefaultConfig() throws IOException {
        StringBuilder content = new StringBuilder();
        InputStream in = getClass().getResourceAsStream("/config.yml");
        assertTrue(in != null, "config.yml is missing from the core resources");
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, Charset.forName("UTF-8")));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append('\n');
            }
        } finally {
            reader.close();
        }
        return content.toString();
    }
}
