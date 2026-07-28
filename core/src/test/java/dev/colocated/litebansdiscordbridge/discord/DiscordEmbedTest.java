package dev.colocated.litebansdiscordbridge.discord;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscordEmbedTest {

    @Test
    @DisplayName("hex, decimal and malformed colours all produce a usable value")
    void parsesColours() {
        assertEquals(16711680, new DiscordEmbed().setColor("#FF0000").toJson().get("color").getAsInt());
        assertEquals(16711680, new DiscordEmbed().setColor("16711680").toJson().get("color").getAsInt());
        assertFalse(new DiscordEmbed().setColor("not-a-colour").toJson().has("color"));
        assertFalse(new DiscordEmbed().setColor((String) null).toJson().has("color"));
        assertFalse(new DiscordEmbed().setColor("").toJson().has("color"));
    }

    @Test
    @DisplayName("only the parts that were set are serialised")
    void omitsUnsetParts() {
        JsonObject json = new DiscordEmbed().setTitle("Player Banned").toJson();

        assertEquals("Player Banned", json.get("title").getAsString());
        assertFalse(json.has("description"));
        assertFalse(json.has("footer"));
        assertFalse(json.has("fields"));
    }

    @Test
    @DisplayName("nested embed parts serialise to Discord's field names")
    void serialisesNestedParts() {
        JsonObject json = new DiscordEmbed()
            .setDescription("**Rewaked_** has been muted.")
            .setAuthor("zxqld", "https://example.invalid", "https://example.invalid/icon.png")
            .setFooter("Punishment #230", "https://example.invalid/footer.png")
            .setThumbnail("https://example.invalid/head.png")
            .setImage("https://example.invalid/banner.png")
            .setTimestamp(1_700_000_000_000L)
            .addField("Player", "Rewaked_", true)
            .addField("Reason", "ignore", false)
            .toJson();

        assertEquals("**Rewaked_** has been muted.", json.get("description").getAsString());
        assertEquals("zxqld", json.getAsJsonObject("author").get("name").getAsString());
        assertEquals("https://example.invalid/icon.png",
            json.getAsJsonObject("author").get("icon_url").getAsString());
        assertEquals("Punishment #230", json.getAsJsonObject("footer").get("text").getAsString());
        assertEquals("https://example.invalid/head.png",
            json.getAsJsonObject("thumbnail").get("url").getAsString());
        assertEquals("https://example.invalid/banner.png",
            json.getAsJsonObject("image").get("url").getAsString());
        assertEquals("2023-11-14T22:13:20Z", json.get("timestamp").getAsString());

        assertEquals(2, json.getAsJsonArray("fields").size());
        JsonObject firstField = json.getAsJsonArray("fields").get(0).getAsJsonObject();
        assertEquals("Player", firstField.get("name").getAsString());
        assertEquals("Rewaked_", firstField.get("value").getAsString());
        assertTrue(firstField.get("inline").getAsBoolean());
    }
}
