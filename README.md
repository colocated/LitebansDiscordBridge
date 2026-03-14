# LiteBansDiscordBridge

A multi-platform plugin that sends LiteBans punishment notifications to Discord webhooks with fully customizable embeds.

Supports **Bukkit / Spigot / Paper** (1.8.8–1.21.x), **Velocity 3+**, and **BungeeCord**.

## Features

- Listens to LiteBans events: Ban, Mute, Warn, and Kick
- Sends rich embed notifications to Discord webhooks
- Fully configurable messages with placeholder support
- Customizable embed colors, fields, author, footer, thumbnail, and more
- Individual configuration for each event type

## Requirements

| Platform | Minimum version | Java |
|---|---|---|
| Bukkit / Spigot / Paper | 1.8.8 | 8+ |
| Velocity | 3.0 | 11+ |
| BungeeCord | Any modern build | 8+ |

LiteBans must be installed on the same server/proxy.

## Installation

### Option A — Download a release (recommended)

1. Go to the [Releases](../../releases) page and download the JAR for your platform and Java version:

   | Platform | Java 8+ (recommended) | Java 21 only |
   |---|---|---|
   | Bukkit / Spigot / Paper | `*-bukkit-*-java8.jar` | `*-bukkit-*-java21.jar` |
   | Velocity 3+ | `*-velocity-*-java8.jar` | `*-velocity-*-java21.jar` |
   | BungeeCord | `*-bungeecord-*-java8.jar` | `*-bungeecord-*-java21.jar` |

   > The **java8** JARs run on any server regardless of Java version and are recommended for most users.
   > The **java21** JARs require a Java 21 runtime but may have minor optimisations for modern servers.

2. Place the JAR in your server's `plugins/` folder.

3. Restart the server/proxy.

4. Configure the webhook URL in `plugins/litebansdiscordbridge/config.yml`.

### Option B — Build from source

#### 1. Install prerequisites

You need a **Java 8+ JDK** to build the java8 JARs, or a **Java 21 JDK** if you also want the java21 JARs. You also need **Maven 3.6+**.

**Windows**

Using [Chocolatey](https://chocolatey.org):
```powershell
choco install maven
```

Using [Scoop](https://scoop.sh):
```powershell
scoop install main/maven
```

Or download the Maven binary zip from [maven.apache.org](https://maven.apache.org/download.cgi), extract it, and add the `bin/` directory to your `PATH`.

**macOS**

Using [Homebrew](https://brew.sh):
```bash
brew install maven
```

Using [SDKMAN](https://sdkman.io):
```bash
sdk install maven
```

**Linux**

Using SDKMAN (recommended — gets the latest version):
```bash
sdk install maven
```

Debian / Ubuntu:
```bash
sudo apt install maven
```

Fedora / RHEL:
```bash
sudo dnf install maven
```

#### 2. Clone and build

```bash
git clone https://github.com/colocated/LitebansDiscordBridge.git
cd LitebansDiscordBridge
```

Build Java 8 JARs (broadest compatibility — runs on any server):
```bash
mvn clean package
```

Build Java 21 JARs (requires Java 21 JDK):
```bash
mvn clean package -P modern
```

#### 3. Locate the built JARs

After a successful build, the JARs are in each platform's `target/` directory:

| Platform | Java 8 JAR | Java 21 JAR |
|---|---|---|
| Bukkit / Spigot / Paper | `bukkit/target/LiteBansDiscordBridge-bukkit-{version}.jar` | `bukkit/target/LiteBansDiscordBridge-bukkit-{version}-modern.jar` |
| Velocity | `velocity/target/LiteBansDiscordBridge-velocity-{version}.jar` | `velocity/target/LiteBansDiscordBridge-velocity-{version}-modern.jar` |
| BungeeCord | `bungeecord/target/LiteBansDiscordBridge-bungeecord-{version}.jar` | `bungeecord/target/LiteBansDiscordBridge-bungeecord-{version}-modern.jar` |

Copy the appropriate JAR to your server's `plugins/` folder and restart.

## Configuration

The plugin creates a default configuration file on first run:

- **Bukkit / Spigot / Paper**: `plugins/litebansdiscordbridge/config.yml`
- **Velocity**: `plugins/litebansdiscordbridge/config.yml`
- **BungeeCord**: `plugins/litebansdiscordbridge/config.yml`

To reload the configuration without restarting, run:
```
/litebansdiscord reload
```
Aliases: `/ldb reload`, `/litebansbridge reload` (Bukkit/BungeeCord only)

### Setting up Discord Webhook

1. Go to your Discord server settings
2. Navigate to Integrations > Webhooks
3. Click "New Webhook"
4. Configure the webhook (name, channel, avatar)
5. Copy the webhook URL
6. Paste it in the `webhook-url` field in `config.yml`

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
| `%id_random%` | The punishment ID with random salting (e.g., 4AC6DA) (output configured by LiteBans) |
| `%active%` | Whether the punishment is active (true/false) |
| `%permanent%` | Whether the punishment is permanent (true/false) |
| `%silent%` | Whether the punishment was silent (true/false) |
| `%ipban%` | Whether the punishment is an IP ban (true/false) |
| `%removed_by_name%` | Who removed the punishment (for unban/unmute events) |
| `%removed_by_uuid%` | UUID of who removed the punishment (for unban/unmute events) |
| `%removed_reason%` | Reason for removing the punishment (for unban/unmute events) |

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

To remove a notification for a specific event, delete the block.
To remove a field (e.g. author) on an embed, delete the sub-block.

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

## Support

For issues or questions, please open an issue on the GitHub repository.

## License

This project is provided as-is for use with LiteBans.
