package dev.colocated.litebansdiscordbridge.listener;

import dev.colocated.litebansdiscordbridge.config.ConfigManager;
import dev.colocated.litebansdiscordbridge.discord.DiscordEmbed;
import dev.colocated.litebansdiscordbridge.discord.DiscordWebhookSender;
import dev.colocated.litebansdiscordbridge.platform.PlatformAdapter;
import dev.colocated.litebansdiscordbridge.util.PlaceholderReplacer;
import litebans.api.Entry;
import litebans.api.Events;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class LiteBansListener extends Events.Listener {

    private final ConfigManager configManager;
    private final DiscordWebhookSender webhookSender;
    private final PlatformAdapter platform;

    public LiteBansListener(ConfigManager configManager, DiscordWebhookSender webhookSender, PlatformAdapter platform) {
        this.configManager = configManager;
        this.webhookSender = webhookSender;
        this.platform = platform;
    }

    @Override
    public void entryAdded(Entry entry) {
        if (entry == null) return;

        String type = entry.getType();
        if (type == null) return;

        String eventType = type.toLowerCase();
        CommentedConfigurationNode eventConfig = configManager.getNode("events." + eventType);

        if (eventConfig.virtual()) return;
        if (!eventConfig.node("enabled").getBoolean(false)) return;

        String webhookUrl = resolveWebhookUrl(eventConfig);
        if (webhookUrl == null) {
            platform.logWarn("Webhook URL is not configured for event type: " + eventType);
            return;
        }

        sendDiscordNotification(entry, eventConfig, webhookUrl);
    }

    @Override
    public void entryRemoved(Entry entry) {
        if (entry == null) return;

        String type = entry.getType();
        if (type == null) return;

        String eventType;
        switch (type.toLowerCase()) {
            case "ban":  eventType = "unban";  break;
            case "mute": eventType = "unmute"; break;
            default: return; // only unban/unmute are handled
        }

        CommentedConfigurationNode eventConfig = configManager.getNode("events." + eventType);

        if (eventConfig.virtual()) return;
        if (!eventConfig.node("enabled").getBoolean(false)) return;

        String webhookUrl = resolveWebhookUrl(eventConfig);
        if (webhookUrl == null) {
            platform.logWarn("Webhook URL is not configured for event type: " + eventType);
            return;
        }

        sendDiscordNotification(entry, eventConfig, webhookUrl);
    }

    @Override
    public void broadcastSent(String message, String type) {
        // Not used
    }

    private String resolveWebhookUrl(CommentedConfigurationNode eventConfig) {
        String eventUrl = eventConfig.node("webhook-url").getString("");
        if (!eventUrl.isEmpty() && isValidHttpsUrl(eventUrl)) {
            return eventUrl;
        }
        String globalUrl = configManager.getString("webhook-url", "");
        if (!globalUrl.isEmpty() && isValidHttpsUrl(globalUrl)) {
            return globalUrl;
        }
        return null;
    }

    private boolean isValidHttpsUrl(String url) {
        return url != null && url.toLowerCase().startsWith("https://");
    }

    private void sendDiscordNotification(Entry entry, CommentedConfigurationNode config, String webhookUrl) {
        CommentedConfigurationNode embedConfig = config.node("embed");
        DiscordEmbed embed = new DiscordEmbed();

        String title = embedConfig.node("title").getString("");
        if (!title.isEmpty()) {
            embed.setTitle(PlaceholderReplacer.replace(title, entry, platform));
        }

        String description = embedConfig.node("description").getString("");
        if (!description.isEmpty()) {
            embed.setDescription(PlaceholderReplacer.replace(description, entry, platform));
        }

        String url = embedConfig.node("url").getString("");
        if (!url.isEmpty()) {
            embed.setUrl(PlaceholderReplacer.replace(url, entry, platform));
        }

        String colorStr = embedConfig.node("color").getString("#FF0000");
        embed.setColor(PlaceholderReplacer.parseColor(colorStr));

        boolean timestamp = embedConfig.node("timestamp").getBoolean(true);
        if (timestamp) {
            embed.setTimestamp(entry.getDateStart());
        }

        CommentedConfigurationNode authorNode = embedConfig.node("author");
        if (!authorNode.virtual()) {
            String authorName     = PlaceholderReplacer.replace(authorNode.node("name").getString(""), entry, platform);
            String authorUrl      = PlaceholderReplacer.replace(authorNode.node("url").getString(""), entry, platform);
            String authorIconUrl  = PlaceholderReplacer.replace(authorNode.node("icon-url").getString(""), entry, platform);
            embed.setAuthor(authorName,
                authorUrl.isEmpty() ? null : authorUrl,
                authorIconUrl.isEmpty() ? null : authorIconUrl);
        }

        CommentedConfigurationNode thumbnailNode = embedConfig.node("thumbnail");
        if (!thumbnailNode.virtual()) {
            String thumbUrl = PlaceholderReplacer.replace(thumbnailNode.node("url").getString(""), entry, platform);
            if (!thumbUrl.isEmpty()) embed.setThumbnail(thumbUrl);
        }

        CommentedConfigurationNode imageNode = embedConfig.node("image");
        if (!imageNode.virtual()) {
            String imgUrl = PlaceholderReplacer.replace(imageNode.node("url").getString(""), entry, platform);
            if (!imgUrl.isEmpty()) embed.setImage(imgUrl);
        }

        CommentedConfigurationNode footerNode = embedConfig.node("footer");
        if (!footerNode.virtual()) {
            String footerText    = PlaceholderReplacer.replace(footerNode.node("text").getString(""), entry, platform);
            String footerIconUrl = PlaceholderReplacer.replace(footerNode.node("icon-url").getString(""), entry, platform);
            if (!footerText.isEmpty()) {
                embed.setFooter(footerText, footerIconUrl.isEmpty() ? null : footerIconUrl);
            }
        }

        CommentedConfigurationNode fieldsNode = embedConfig.node("fields");
        if (!fieldsNode.virtual() && fieldsNode.isList()) {
            for (CommentedConfigurationNode fieldNode : fieldsNode.childrenList()) {
                String fieldName  = PlaceholderReplacer.replace(fieldNode.node("name").getString(""), entry, platform);
                String fieldValue = PlaceholderReplacer.replace(fieldNode.node("value").getString(""), entry, platform);
                boolean inline    = fieldNode.node("inline").getBoolean(false);
                if (!fieldName.isEmpty() && !fieldValue.isEmpty()) {
                    embed.addField(fieldName, fieldValue, inline);
                }
            }
        }

        String content = config.node("content").getString("");
        if (!content.isEmpty()) {
            content = PlaceholderReplacer.replace(content, entry, platform);
        }

        webhookSender.sendEmbed(webhookUrl, embed, content.isEmpty() ? null : content);
    }
}
