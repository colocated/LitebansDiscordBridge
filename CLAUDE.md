# LiteBansDiscordBridge

Maven multi-module plugin bridging LiteBans punishment events to Discord webhooks. Supports Bukkit/Spigot/Paper (1.8.8–1.21.x), BungeeCord, and Velocity 3+.

## Build

```bash
# Java 8 JARs (broadest compatibility — default). Runs the test suite first.
mvn clean package

# Java 21 JARs
mvn clean package -P modern

# Tests only
mvn test

# Confirm shipped JARs contain nothing newer than the target release
python3 scripts/verify-bytecode.py 8 */target/LiteBansDiscordBridge-*-{version}.jar
```

Output JARs after build:
- `bukkit/target/LiteBansDiscordBridge-bukkit-{version}.jar`
- `bungeecord/target/LiteBansDiscordBridge-bungeecord-{version}.jar`
- `velocity/target/LiteBansDiscordBridge-velocity-{version}.jar`

Both profiles build on a JDK 21 toolchain; only the bytecode target differs. On JDK 9+ the
`release-flag` profile (auto-activated) passes `--release`, so the default build is compiled
against the **Java 8 API** — a `List.of()` or `String.isBlank()` is a compile error there, not a
runtime `NoSuchMethodError` on an old server.

## Tests

JUnit 5 in `core/src/test/java`, run automatically before `package`. LiteBans is stubbed via
`Database.setInstance()` — see `support/TestDatabase`, `support/TestEntry`, `support/TestPlatform`.
`DefaultConfigTest` cross-checks `config.yml` against `PlaceholderContext.KNOWN_PLACEHOLDERS` in
both directions, so a new placeholder must be documented and a documented one must resolve.

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
- `discord.DiscordEmbed` — fluent builder → Gson `JsonObject` via `toJson()`; `setColor(String)` parses `#RRGGBB` or decimal
- `util.PlaceholderContext` — all placeholder values for one `litebans.api.Entry`, built once per event and reused for every part of the embed; `apply(template)` substitutes in a single pass
- `util.PlayerNameResolver` — UUID → username: online player, then `litebans.api.Database.getPlayerName()` (covers offline players), then the UUID; cached with a TTL
- `platform.PlatformAdapter` — interface each platform implements for logging and `getDataDirectory()`

## Conventions

- **Java 8 target** by default (via `--release 8` on a JDK 9+ toolchain); Java 21 via `-P modern` profile in root `pom.xml`
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

1. Add the key to `KNOWN_PLACEHOLDERS` in `PlaceholderContext`
2. Resolve it in the `PlaceholderContext` constructor:
   ```java
   put("new_placeholder", entry.getSomeField(), "default");
   ```
   `put` falls back when the value is null *or* empty, so `%placeholder%` never renders as `null`.
   For anything expensive (a database or network lookup), resolve it lazily like `playerName()`
   instead — the constructor runs for every event, whether or not the config uses the placeholder.
3. Document it in the placeholder list at the top of `core/src/main/resources/config.yml` —
   `DefaultConfigTest` fails if a known placeholder is undocumented, or a documented one is unknown
