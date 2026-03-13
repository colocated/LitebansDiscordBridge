package dev.colocated.litebansdiscordbridge.discord;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class DiscordEmbed {

    private String title;
    private String description;
    private String url;
    private int color;
    private String timestamp;
    private Footer footer;
    private Thumbnail thumbnail;
    private Image image;
    private Author author;
    private final List<Field> fields = new ArrayList<Field>();

    public DiscordEmbed setTitle(String title) {
        this.title = title;
        return this;
    }

    public DiscordEmbed setDescription(String description) {
        this.description = description;
        return this;
    }

    public DiscordEmbed setUrl(String url) {
        this.url = url;
        return this;
    }

    public DiscordEmbed setColor(int color) {
        this.color = color;
        return this;
    }

    public DiscordEmbed setTimestamp(boolean enabled) {
        if (enabled) {
            this.timestamp = Instant.now().toString();
        }
        return this;
    }

    public DiscordEmbed setTimestamp(long epochMillis) {
        this.timestamp = Instant.ofEpochMilli(epochMillis).toString();
        return this;
    }

    public DiscordEmbed setFooter(String text, String iconUrl) {
        this.footer = new Footer(text, iconUrl);
        return this;
    }

    public DiscordEmbed setThumbnail(String url) {
        this.thumbnail = new Thumbnail(url);
        return this;
    }

    public DiscordEmbed setImage(String url) {
        this.image = new Image(url);
        return this;
    }

    public DiscordEmbed setAuthor(String name, String url, String iconUrl) {
        this.author = new Author(name, url, iconUrl);
        return this;
    }

    public DiscordEmbed addField(String name, String value, boolean inline) {
        fields.add(new Field(name, value, inline));
        return this;
    }

    public JsonObject toJson() {
        JsonObject embed = new JsonObject();

        if (title != null) embed.addProperty("title", title);
        if (description != null) embed.addProperty("description", description);
        if (url != null) embed.addProperty("url", url);
        if (color != 0) embed.addProperty("color", color);
        if (timestamp != null) embed.addProperty("timestamp", timestamp);

        if (footer != null) {
            JsonObject footerObj = new JsonObject();
            footerObj.addProperty("text", footer.text);
            if (footer.iconUrl != null) footerObj.addProperty("icon_url", footer.iconUrl);
            embed.add("footer", footerObj);
        }

        if (thumbnail != null) {
            JsonObject thumbObj = new JsonObject();
            thumbObj.addProperty("url", thumbnail.url);
            embed.add("thumbnail", thumbObj);
        }

        if (image != null) {
            JsonObject imageObj = new JsonObject();
            imageObj.addProperty("url", image.url);
            embed.add("image", imageObj);
        }

        if (author != null) {
            JsonObject authorObj = new JsonObject();
            authorObj.addProperty("name", author.name);
            if (author.url != null) authorObj.addProperty("url", author.url);
            if (author.iconUrl != null) authorObj.addProperty("icon_url", author.iconUrl);
            embed.add("author", authorObj);
        }

        if (!fields.isEmpty()) {
            JsonArray fieldsArray = new JsonArray();
            for (Field field : fields) {
                JsonObject fieldObj = new JsonObject();
                fieldObj.addProperty("name", field.name);
                fieldObj.addProperty("value", field.value);
                fieldObj.addProperty("inline", field.inline);
                fieldsArray.add(fieldObj);
            }
            embed.add("fields", fieldsArray);
        }

        return embed;
    }

    // Static inner classes replacing Java 16 records for Java 8 compatibility
    private static final class Footer {
        final String text;
        final String iconUrl;
        Footer(String text, String iconUrl) { this.text = text; this.iconUrl = iconUrl; }
    }

    private static final class Thumbnail {
        final String url;
        Thumbnail(String url) { this.url = url; }
    }

    private static final class Image {
        final String url;
        Image(String url) { this.url = url; }
    }

    private static final class Author {
        final String name;
        final String url;
        final String iconUrl;
        Author(String name, String url, String iconUrl) { this.name = name; this.url = url; this.iconUrl = iconUrl; }
    }

    private static final class Field {
        final String name;
        final String value;
        final boolean inline;
        Field(String name, String value, boolean inline) { this.name = name; this.value = value; this.inline = inline; }
    }
}
