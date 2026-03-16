---
name: build-release
description: Runs mvn clean package (and optionally -P modern) and identifies the correct output JAR paths under bukkit/target/, bungeecord/target/, and velocity/target/. Use when user says 'build', 'package', 'release', 'create JAR', or 'deploy'. Reminds about shade relocations and the jar.suffix property. Do NOT use for dependency updates, adding new modules, or platform-specific code changes.
---
# build-release

## Critical

- **Never** alter shade relocations in `pom.xml` — all deps must remain under `dev.colocated.litebansdiscordbridge.libs.*` or class conflicts will occur at runtime.
- SnakeYAML is pinned to `1.33` in `<dependencyManagement>`. Do not bump it; Configurate 4.1.2 requires exactly this version or you'll get `NoSuchMethodError: ParserImpl`.
- Two profiles exist — default (Java 8) and `modern` (Java 21). Pick the right one for the deployment target before building.

## Instructions

1. **Confirm the target Java version.** Ask or check the server environment:
   - Java 8–20 server → use default profile
   - Java 21+ server → use `-P modern` profile

2. **Run the build** from the repo root:
   ```bash
   # Java 8 JARs (broadest compatibility — 1.8.8 through 1.21.x, all proxies)
   mvn clean package

   # Java 21 JARs
   mvn clean package -P modern
   ```
   Verify: build output ends with `BUILD SUCCESS` before proceeding.

3. **Locate output JARs.** The `finalName` in each module's `pom.xml` uses `${jar.suffix}` (empty for default, `-modern` for `-P modern`). Current version is `2.0.0`:
   ```
   bukkit/target/LiteBansDiscordBridge-bukkit-2.0.0.jar          # default
   bukkit/target/LiteBansDiscordBridge-bukkit-2.0.0-modern.jar   # -P modern

   bungeecord/target/LiteBansDiscordBridge-bungeecord-2.0.0.jar
   bungeecord/target/LiteBansDiscordBridge-bungeecord-2.0.0-modern.jar

   velocity/target/LiteBansDiscordBridge-velocity-2.0.0.jar
   velocity/target/LiteBansDiscordBridge-velocity-2.0.0-modern.jar
   ```
   Verify: each JAR exists and is non-zero bytes (`ls -lh bukkit/target/*.jar bungeecord/target/*.jar velocity/target/*.jar`).

4. **Confirm shade relocations are intact** (spot-check one JAR):
   ```bash
   jar tf bukkit/target/LiteBansDiscordBridge-bukkit-2.0.0.jar | grep okhttp3
   # Expected: dev/colocated/litebansdiscordbridge/libs/okhttp3/...
   # Bad:      okhttp3/... (means relocation was dropped)
   ```

5. **Deploy** the platform-specific JAR to the correct server `plugins/` or `mods/` folder:
   - Bukkit/Spigot/Paper → `bukkit/target/` JAR
   - BungeeCord → `bungeecord/target/` JAR
   - Velocity → `velocity/target/` JAR

## Examples

User says: "Build a release JAR for a Paper 1.21 server running Java 21"

Actions taken:
1. Java 21 server confirmed → use `-P modern`
2. Run `mvn clean package -P modern`
3. Output: `bukkit/target/LiteBansDiscordBridge-bukkit-2.0.0-modern.jar`
4. Spot-check relocation: `jar tf bukkit/target/LiteBansDiscordBridge-bukkit-2.0.0-modern.jar | grep okhttp3` → confirms `dev/colocated/litebansdiscordbridge/libs/okhttp3/`
5. Drop `LiteBansDiscordBridge-bukkit-2.0.0-modern.jar` into server `plugins/`

Result: Paper 1.21 server loads the plugin without class conflicts.

## Common Issues

- **`NoSuchMethodError: org.yaml.snakeyaml.parser.ParserImpl`** — SnakeYAML drifted from `1.33`. Check `mvn dependency:tree | grep snakeyaml`; a transitive dep is pulling in 2.x. Add `<exclusion>` in the offending dep or re-add the pin in `<dependencyManagement>`.

- **`BUILD FAILURE: package does not exist` on `litebans`** — LiteBans API is not in any public repo. Manually install: `mvn install:install-file -Dfile=litebans-api.jar -DgroupId=litebans -DartifactId=api -Dversion=0.6.0 -Dpackaging=jar`.

- **JAR filename has no `-modern` suffix despite using `-P modern`** — The `jar.suffix` property was overridden locally. Check for a `<jar.suffix>` in a child `pom.xml` or a `-Djar.suffix=` flag that's blanking it.

- **Shade warnings about `okhttp3` duplicate classes** — Usually caused by multiple OkHttp versions on the classpath. Run `mvn dependency:tree | grep okhttp` and enforce a single version via `<dependencyManagement>`.

- **`velocity/target/` JAR is missing** — Velocity module compiles against Velocity API 3+; if `papermc` repo is unreachable the build silently skips. Confirm internet access to `https://repo.papermc.io` or use a local Maven mirror.