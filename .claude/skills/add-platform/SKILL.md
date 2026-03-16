---
name: add-platform
description: Creates a new platform module (pom.xml, PlatformAdapter impl, main plugin class, command class, and platform descriptor) following the project's exact multi-module Maven patterns. Use when user says 'add platform', 'support new server type', 'port to X', or creates files under a new top-level module directory. Do NOT use for modifying existing platform modules (bukkit, bungeecord, velocity).
---
# Add Platform

## Critical

- All shaded deps (okhttp3, gson, configurate, snakeyaml) **must** be declared as `compile`-scope dependencies in the new module's `pom.xml` — they are relocated by the parent shade config. Never mark them `provided`.
- The `maven-shade-plugin` execution **must** be present in every platform module's `<build>` — it is NOT inherited automatically.
- `webhookSender.shutdown()` **must** be called in the disable/shutdown lifecycle or the OkHttp dispatcher thread will leak.
- `Events.get().unregister(liteBansListener)` **must** be called before `shutdown()` in the disable lifecycle.
- Add the new module to `<modules>` in the root `pom.xml` before building.

## Instructions

1. **Determine the platform slug** (lowercase, e.g. `sponge`). All paths below use `<slug>` as a placeholder.
   - Java package: `dev.colocated.litebansdiscordbridge.<slug>`
   - Artifact ID: `litebansdiscordbridge-<slug>`
   - Final JAR name: `LiteBansDiscordBridge-<slug>-${project.version}${jar.suffix}`
   - Class prefix: e.g. `Sponge` → `SpongePlatformAdapter`, `LiteBansDiscordBridgeSponge`, `SpongeCommand`

2. **Register the module in the root `pom.xml`** (`pom.xml` lines 15–20):
   ```xml
   <module><slug></module>
   ```
   Add it inside `<modules>` alongside `core`, `bukkit`, `velocity`, `bungeecord`.
   Verify: `<modules>` now contains the new entry.

3. **Create `<slug>/pom.xml`** — copy the structure from `bukkit/pom.xml`, replacing:
   - `<artifactId>litebansdiscordbridge-<slug></artifactId>`
   - `<name>LiteBansDiscordBridge-Slug</name>`
   - `<description>Slug platform module</description>`
   - `<finalName>LiteBansDiscordBridge-<slug>-${project.version}${jar.suffix}</finalName>`
   - Replace the `spigot-api` `<provided>` dependency with the new platform's API at `provided` scope
   - Keep `litebansdiscordbridge-core`, `litebans:api`, `okhttp`, `gson`, `configurate-yaml` exactly as in bukkit's pom
   - Keep the `maven-shade-plugin` execution block unchanged

   Verify: `mvn validate -pl <slug>` reports no errors.

4. **Create the source tree** under `<slug>/src/main/java/dev/colocated/litebansdiscordbridge/<slug>/`:
   - `<Slug>PlatformAdapter.java`
   - `LiteBansDiscordBridge<Slug>.java`
   - `<Slug>Command.java`

5. **Implement `<Slug>PlatformAdapter`** — implement `PlatformAdapter` (`core/.../platform/PlatformAdapter.java`):
   ```java
   package dev.colocated.litebansdiscordbridge.<slug>;

   import dev.colocated.litebansdiscordbridge.platform.PlatformAdapter;
   import java.nio.file.Path;
   import java.util.Optional;
   import java.util.UUID;

   public class <Slug>PlatformAdapter implements PlatformAdapter {
       // store logger + dataDirectory as fields
       @Override public Optional<String> getOnlinePlayerName(UUID uuid) { /* platform player lookup */ }
       @Override public Path getDataDirectory() { return dataDirectory; }
       @Override public void logInfo(String message) { /* platform logger.info */ }
       @Override public void logWarn(String message) { /* platform logger.warn/warning */ }
       @Override public void logError(String message, Throwable throwable) {
           if (throwable != null) { /* log with cause */ } else { /* log message only */ }
       }
   }
   ```
   See `BukkitPlatformAdapter` (uses `Bukkit.getPlayer`) or `VelocityPlatformAdapter` (uses `server.getPlayer`) as reference.
   Verify: class compiles with no missing-method errors.

6. **Implement the main plugin class** — wire lifecycle following the platform's conventions:

   *Bukkit-like* (extends `JavaPlugin`, `onEnable`/`onDisable`):
   ```java
   platform = new <Slug>PlatformAdapter(this);
   configManager = new ConfigManager(platform.getDataDirectory());
   configManager.load();
   webhookSender = new DiscordWebhookSender(platform);
   liteBansListener = new LiteBansListener(configManager, webhookSender, platform);
   Events.get().register(liteBansListener);
   // register command
   ```

   *Velocity-like* (annotation `@Plugin`, `@Subscribe ProxyInitializeEvent`/`ProxyShutdownEvent`, `@Inject` constructor):
   Replicate `LiteBansDiscordBridgeVelocity` — inject `ProxyServer`, `Logger`, `@DataDirectory Path`.

   Disable/shutdown must null-check then call `Events.get().unregister(liteBansListener)` and `webhookSender.shutdown()`.

7. **Implement `<Slug>Command`** — handle the `reload` subcommand:
   - Bukkit-like: implement `CommandExecutor` + `TabCompleter`; check `sender.hasPermission("litebansdiscord.reload")`
   - Velocity-like: implement `SimpleCommand`; use `Component.text(...)` + `NamedTextColor`; override `hasPermission` to check `litebansdiscord.command`
   - On success: call `configManager.reload()`, send green success message, call `platform.logInfo("Configuration reloaded by " + <senderName>)`
   - On failure: send red error message, call `platform.logError("Failed to reload configuration", e)`

8. **Create the platform descriptor** under `<slug>/src/main/resources/`:

   *Bukkit (`plugin.yml`)* — copy `bukkit/src/main/resources/plugin.yml`, change only `main:` to the new main class.

   *BungeeCord (`bungee.yml`)* — copy `bungeecord/src/main/resources/bungee.yml`, change `main:`.

   *Velocity* — no file; descriptor is the `@Plugin` annotation on the main class.

   Verify: `filtering: true` is set in pom `<resources>` so `${project.version}` expands.

9. **Build and verify**:
   ```bash
   mvn clean package -pl <slug> -am
   ```
   Confirm `<slug>/target/LiteBansDiscordBridge-<slug>-<version>.jar` exists and is non-empty.

## Examples

**User says:** "Add a Sponge platform module"

**Actions taken:**
1. Add `<module>sponge</module>` to root `pom.xml`
2. Create `sponge/pom.xml` with `artifactId=litebansdiscordbridge-sponge`, `spongeapi` at `provided` scope, shade plugin execution
3. Create `SpongePlatformAdapter` implementing all 5 `PlatformAdapter` methods using SpongeAPI's `Game`/`Logger`
4. Create `LiteBansDiscordBridgeSponge` with `@Plugin` annotation and lifecycle `@Listener` methods calling the standard wiring pattern
5. Create `SpongeCommand` implementing the reload subcommand with permission check
6. No descriptor file — Sponge uses annotation-based plugin metadata
7. Run `mvn clean package -pl sponge -am`

**Result:** `sponge/target/LiteBansDiscordBridge-sponge-2.0.0.jar`

## Common Issues

- **`NoSuchMethodError: org.yaml.snakeyaml.parser.ParserImpl`** at startup: SnakeYAML 2.x was resolved transitively. Verify root `pom.xml` `<dependencyManagement>` pins `org.yaml:snakeyaml:1.33`; it applies to all modules automatically.

- **`ClassNotFoundException: dev.colocated.litebansdiscordbridge.libs.okhttp3...`**: The shade plugin execution is missing from `<slug>/pom.xml`. Add the `maven-shade-plugin` `<execution>` block exactly as in `bukkit/pom.xml:61-71`.

- **`mvn clean package -pl sponge -am` skips the module**: The `<module>sponge</module>` entry is missing from root `pom.xml`. Add it inside `<modules>` and re-run.

- **`Events.get().register(...)` throws at enable**: LiteBans is not listed as a dependency in the platform descriptor. Add `LiteBans` to `depend:` (Bukkit/BungeeCord) or `@Dependency(id = "litebans")` (Velocity) so LiteBans loads first.

- **OkHttp dispatcher thread leak on server stop**: `webhookSender.shutdown()` was not called in the disable/shutdown handler. It must be called unconditionally (after a null check) even if `liteBansListener` failed to init.