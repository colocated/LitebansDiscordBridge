package dev.colocated.litebansdiscordbridge.listener;

import dev.colocated.litebansdiscordbridge.LiteBansDiscordBridge;
import dev.colocated.litebansdiscordbridge.discord.DiscordEmbed;
import dev.colocated.litebansdiscordbridge.util.PlaceholderReplacer;
import litebans.api.Entry;
import litebans.api.Events;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class LiteBansListener extends Events.Listener {

    private final LiteBansDiscordBridge plugin;

    public LiteBansListener(LiteBansDiscordBridge plugin) {
        this.plugin = plugin;
    }

    @Override
    public void entryAdded(Entry entry) {
        if (entry == null) {
            return;
        }

        String type = entry.getType();
        if (type == null) {
            return;
        }

        String eventType = type.toLowerCase();
        String configPath = "events." + eventType;

        CommentedConfigurationNode eventConfig = plugin.getConfigManager().getNode(configPath);

        // If the event config doesn't exist, it's disabled
        if (eventConfig.virtual()) {
            return;
        }

        String webhookUrl = plugin.getConfigManager().getString("webhook-url", "");
        if (webhookUrl.isEmpty()) {
            plugin.getLogger().warn("Webhook URL is not configured!");
            return;
        }

        sendDiscordNotification(entry, eventConfig, webhookUrl);
    }

    @Override
    public void entryRemoved(Entry entry) {
        // Not needed for this plugin
    }

    @Override
    public void broadcastSent(String message, String type) {
        // Not needed for this plugin
    }

    private void sendDiscordNotification(Entry entry, CommentedConfigurationNode config, String webhookUrl) {
        CommentedConfigurationNode embedConfig = config.node("embed");

        DiscordEmbed embed = new DiscordEmbed();

        String title = embedConfig.node("title").getString("");
        if (!title.isEmpty()) {
            embed.setTitle(PlaceholderReplacer.replace(title, entry, plugin.getServer()));
        }

        String description = embedConfig.node("description").getString("");
        if (!description.isEmpty()) {
            embed.setDescription(PlaceholderReplacer.replace(description, entry, plugin.getServer()));
        }

        String url = embedConfig.node("url").getString("");
        if (!url.isEmpty()) {
            embed.setUrl(PlaceholderReplacer.replace(url, entry, plugin.getServer()));
        }

        String colorStr = embedConfig.node("color").getString("#FF0000");
        embed.setColor(PlaceholderReplacer.parseColor(colorStr));

        // Add Discord timestamp (shows relative time like "5 minutes ago" and is searchable)
        boolean timestamp = embedConfig.node("timestamp").getBoolean(true);
        if (timestamp) {
            embed.setTimestamp(entry.getDateStart());
        }

        // Author section - only include if the block exists
        CommentedConfigurationNode authorNode = embedConfig.node("author");
        if (!authorNode.virtual()) {
            String authorName = PlaceholderReplacer.replace(authorNode.node("name").getString(""), entry, plugin.getServer());
            String authorUrl = PlaceholderReplacer.replace(authorNode.node("url").getString(""), entry, plugin.getServer());
            String authorIconUrl = PlaceholderReplacer.replace(authorNode.node("icon-url").getString(""), entry, plugin.getServer());
            embed.setAuthor(authorName, authorUrl.isEmpty() ? null : authorUrl, authorIconUrl.isEmpty() ? null : authorIconUrl);
        }

        // Thumbnail - only include if the block exists
        CommentedConfigurationNode thumbnailNode = embedConfig.node("thumbnail");
        if (!thumbnailNode.virtual()) {
            String thumbnailUrl = PlaceholderReplacer.replace(thumbnailNode.node("url").getString(""), entry, plugin.getServer());
            if (!thumbnailUrl.isEmpty()) {
                embed.setThumbnail(thumbnailUrl);
            }
        }

        // Image - only include if the block exists
        CommentedConfigurationNode imageNode = embedConfig.node("image");
        if (!imageNode.virtual()) {
            String imageUrl = PlaceholderReplacer.replace(imageNode.node("url").getString(""), entry, plugin.getServer());
            if (!imageUrl.isEmpty()) {
                embed.setImage(imageUrl);
            }
        }

        // Footer - only include if the block exists
        CommentedConfigurationNode footerNode = embedConfig.node("footer");
        if (!footerNode.virtual()) {
            String footerText = PlaceholderReplacer.replace(footerNode.node("text").getString(""), entry, plugin.getServer());
            String footerIconUrl = PlaceholderReplacer.replace(footerNode.node("icon-url").getString(""), entry, plugin.getServer());
            if (!footerText.isEmpty()) {
                embed.setFooter(footerText, footerIconUrl.isEmpty() ? null : footerIconUrl);
            }
        }

        // Fields - add all fields that exist in the list
        CommentedConfigurationNode fieldsNode = embedConfig.node("fields");
        if (!fieldsNode.virtual() && fieldsNode.isList()) {
            for (CommentedConfigurationNode fieldNode : fieldsNode.childrenList()) {
                String fieldName = PlaceholderReplacer.replace(fieldNode.node("name").getString(""), entry, plugin.getServer());
                String fieldValue = PlaceholderReplacer.replace(fieldNode.node("value").getString(""), entry, plugin.getServer());
                boolean inline = fieldNode.node("inline").getBoolean(false);

                if (!fieldName.isEmpty() && !fieldValue.isEmpty()) {
                    embed.addField(fieldName, fieldValue, inline);
                }
            }
        }

        String content = config.node("content").getString("");
        if (!content.isEmpty()) {
            content = PlaceholderReplacer.replace(content, entry, plugin.getServer());
        }

        plugin.getWebhookSender().sendEmbed(webhookUrl, embed, content.isEmpty() ? null : content);
    }
}
