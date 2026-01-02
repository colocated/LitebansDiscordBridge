# LiteBansDiscordBridge

A Velocity plugin that sends LiteBans punishment notifications to Discord webhooks with fully customizable embeds.

## Features

- Listens to LiteBans events: Ban, Mute, Warn, and Kick
- Sends rich embed notifications to Discord webhooks
- Fully configurable messages with placeholder support
- Customizable embed colors, fields, author, footer, thumbnail, and more
- Individual configuration for each event type

## Requirements

- Velocity (API Version 3.4.0) or higher
- LiteBans plugin with API access
- Java 21
- Maven (for building)

## Installation

1. Install Maven (if not already installed):
   ```bash
   sdk install maven
   ```

2. Build the plugin:
   ```bash
   mvn clean package
   ```

3. The compiled JAR will be in `target/LiteBansDiscordBridge-1.0.0.jar`

4. Copy the JAR to your Velocity `plugins/` folder

5. Restart your Velocity proxy

6. Configure the webhook URL in `plugins/litebansdiscordbridge/config.yml`

## Configuration

The plugin creates a default configuration file at `plugins/litebansdiscordbridge/config.yml` on first run.

### Setting up Discord Webhook

1. Go to your Discord server settings
2. Navigate to Integrations > Webhooks
3. Click "New Webhook"
4. Configure the webhook (name, channel, avatar)
5. Copy the webhook URL
6. Paste it in the `webhook-url` field in config.yml

### Available Placeholders

All messages support the following placeholders:

| Placeholder | Description |
|------------|-------------|
| `%player%` / `%player_name%` / `%player_uuid%` | The punished player |
| `%executor%` / `%executor_name%` / `%executor_uuid%` | Who issued the punishment |
| `%reason%` | The reason for the punishment |
| `%type%` | The type of punishment (ban, mute, warn, kick) |
| `%duration%` | How long the punishment lasts (time remaining) |
| `%duration_original%` | The original duration |
| `%server%` / `%server_origin%` | The server where the punishment was issued |
| `%server_scope%` | The scope of the punishment (Global or specific server) |
| `%date%` / `%date_start%` | When the punishment was issued |
| `%date_end%` | When the punishment expires |
| `%ip%` / `%ip_address%` | The IP address of the punished player |
| `%id%` | The punishment ID |
| `%active%` | Whether the punishment is active (true/false) |
| `%permanent%` | Whether the punishment is permanent (true/false) |
| `%silent%` | Whether the punishment was silent (true/false) |
| `%ipban%` | Whether the punishment is an IP ban (true/false) |

### Customization Options

Each event type (ban, mute, warn, kick) can be customized with:

- **content**: Optional message content (appears above the embed)
- **embed.title**: The embed title
- **embed.description**: The embed description
- **embed.url**: Optional URL when clicking the title
- **embed.color**: Color in hex (#RRGGBB) or decimal format
- **embed.timestamp**: Whether to include a timestamp
- **embed.author**: Author section with name, URL, and icon
- **embed.thumbnail**: Small image on the top right
- **embed.image**: Large image in the embed
- **embed.footer**: Footer text and icon
- **embed.fields**: List of information fields with name, value, and inline properties

To remove a notification for a specific event, just delete the block.
To remove a field (e.g. author) on an embed, just delete the sub-block.

### Example Configuration

```yaml
webhook-url: "https://discord.com/api/webhooks/YOUR_WEBHOOK_ID/YOUR_WEBHOOK_TOKEN"

events:
  ban:
    content: ""
    embed:
      title: "Player Banned"
      description: "**%player%** has been banned from the server."
      color: "#FF0000"
      timestamp: true

      author:
        name: "%executor%"
        icon-url: "https://crafatar.com/avatars/%executor_uuid%?overlay"

      thumbnail:
        url: "https://crafatar.com/avatars/%player_uuid%?overlay"

      fields:
        - name: "Player"
          value: "%player%"
          inline: true
        - name: "Reason"
          value: "%reason%"
          inline: false
```

## Building

```bash
mvn clean package
```

The compiled plugin will be available at `target/LiteBansDiscordBridge-1.0.0.jar`

## Support

For issues or questions, please open an issue on the GitHub repository.

## License

This project is provided as-is for use with Velocity and LiteBans.
