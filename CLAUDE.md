# LiteBansDiscordBridge

Maven multi-module plugin bridging LiteBans punishment events to Discord webhooks. Supports Bukkit/Spigot/Paper (1.8.8–1.21.x), BungeeCord, and Velocity 3+.

## Build

```bash
# Java 8 JARs (broadest compatibility — default)
mvn clean package

# Java 21 JARs
mvn clean package -P modern
```

Output JARs after build:
- `bukkit/target/LiteBansDiscordBridge-bukkit-{version}.jar`
- `bungeecord/target/LiteBansDiscordBridge-bungeecord-{version}.jar`
- `velocity/target/LiteBansDiscordBridge-velocity-{version}.jar`

## Architecture

Modules: `core` (platform-agnostic) → `bukkit`, `bungeecord`, `velocity` (platform modules).

| Module | Main class | Platform descriptor |
|--------|-----------|---------------------|
| `core` | — | `core/src/main/resources/config.yml` |
| `bukkit` | `LiteBansDiscordBridgeBukkit` | `bukkit/src/main/resources/plugin.yml` |
| `bungeecord` | `LiteBansDiscordBridgeBungee` | `bungeecord/src/main/resources/bungee.yml` |
| `velocity` | `LiteBansDiscordBridgeVelocity` | `@Plugin` annotation |

**Core classes (all under `dev.colocated.litebansdiscordbridge`):**
- `config.ConfigManager` — loads/reloads `config.yml` via Configurate 4 (`YamlConfigurationLoader`)
- `listener.LiteBansListener` — handles `entryAdded`/`entryRemoved` from `litebans.api.Events`
- `discord.DiscordWebhookSender` — async OkHttp3 POST; call `shutdown()` on plugin disable
- `discord.DiscordEmbed` — fluent builder → Gson `JsonObject` via `toJson()`
- `util.PlaceholderReplacer` — resolves `%player%`, `%executor%`, `%reason%`, etc. from `litebans.api.Entry`
- `platform.PlatformAdapter` — interface each platform implements for logging and `getDataDirectory()`

## Conventions

- **Java 8 source/target** by default; Java 21 via `-P modern` profile in root `pom.xml`
- Shaded deps relocated under `dev.colocated.litebansdiscordbridge.libs.*` (okhttp3, gson, configurate, snakeyaml)
- SnakeYAML pinned to `1.33` in `pom.xml` `<dependencyManagement>` to avoid `NoSuchMethodError` with Configurate 4.1.2
- Event types in `config.yml`: `ban`, `mute`, `warn`, `kick`, `unban`, `unmute` — keyed as `events.<type>`
- Per-event `webhook-url` overrides global `webhook-url` if set and starts with `https://`
- `plugin.yml` sets `api-version: '1.13'`; Velocity uses `@Plugin(id = "litebansdiscordbridge", ...)`
- Command `/litebansdiscord reload` (aliases `ldb`, `litebansbridge`) requires `litebansdiscord.reload` permission

## Adding a New Platform

1. Create `<platform>/pom.xml` inheriting `litebansdiscordbridge-parent`, depend on `litebansdiscordbridge-core`
2. Implement `PlatformAdapter` — see `BukkitPlatformAdapter`, `BungeePlatformAdapter`, `VelocityPlatformAdapter`
3. Wire main plugin class: instantiate `ConfigManager(dataDir)`, `DiscordWebhookSender(platform)`, `LiteBansListener(configManager, webhookSender, platform)`
4. Register: `Events.get().register(liteBansListener)` on enable; `Events.get().unregister(liteBansListener)` on disable; `webhookSender.shutdown()` on disable
5. Add `<module><platform></module>` to root `pom.xml`

## Adding a New Event Type

1. Add block to `core/src/main/resources/config.yml` under `events:` with `enabled`, `content`, and `embed` keys
2. In `LiteBansListener.entryAdded()` map `entry.getType().toLowerCase()` to the new config key
3. For removal events, add a `case` in `entryRemoved()` switch mapping e.g. `"ban"` → `"unban"`

## Adding a New Placeholder

In `PlaceholderReplacer.replace()`:

```java
text = text.replace("%new_placeholder%",
    entry.getSomeField() != null ? entry.getSomeField() : "default");
```
