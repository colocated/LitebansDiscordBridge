---
name: add-event-type
description: Adds a new LiteBans event type (e.g. ipban, ipmute, unwarn) to config.yml and LiteBansListener.java. Use when user says 'add event', 'handle new punishment type', 'add ipban event', 'add ipmute event', 'add unwarn support', or references a new entry type string from LiteBans. Updates entryAdded/entryRemoved switch in LiteBansListener and adds a new events block to core/src/main/resources/config.yml. Do NOT use for modifying embed fields or colors on existing events.
---
# add-event-type

## Critical

- `entryAdded` is already generic — it reads `entry.getType().toLowerCase()` and looks up `events.<type>` in config. **No Java change needed for add-only events** (e.g. `ipban`, `ipmute`, `warn` variants).
- `entryRemoved` uses a hard-coded `switch` that only maps `ban→unban` and `mute→unmute`. **Any new removal event requires adding a new `case` to that switch.**
- The config key under `events:` must exactly match what `entry.getType()` returns in LiteBans (lowercase). Verify the LiteBans type string before naming the config key.
- Never add a `default:` catch-all to the `entryRemoved` switch — removal only fires for explicitly mapped types.

## Instructions

### Step 1 — Identify the event category

Determine which method(s) need to handle the new type:
- **Punishment applied** (ban, mute, warn, kick, ipban, ipmute…): handled by `entryAdded` → config only.
- **Punishment removed** (unban, unmute, unwarn…): handled by `entryRemoved` → config + switch case.

Verify the exact string LiteBans uses for `entry.getType()` (e.g. LiteBans uses `"ipban"` for IP bans).

### Step 2 — Add the config block in `core/src/main/resources/config.yml`

Append a new block under `events:`, following the exact structure of an existing peer event. Use the closest semantic match as your template:
- Punishment events → copy `ban` or `mute` block (has `%duration%` field)
- Kick-like events → copy `kick` block (no `%duration%`)
- Removal events → copy `unban` or `unmute` block (uses `%removed_by_name%`, `%removed_by_uuid%`, `%removed_reason%`)

Minimum required structure:
```yaml
  <type>:
    enabled: true
    # webhook-url: "https://..."
    content: ""
    embed:
      title: "Player <Action>"
      description: "**%player%** has been <actioned>."
      url: ""
      color: "#RRGGBB"
      timestamp: true
      author:
        name: "%executor%"
        url: ""
        icon-url: "https://api.mineatar.io/head/%executor_uuid%"
      thumbnail:
        url: "https://api.mineatar.io/head/%player_uuid%"
      footer:
        text: "LiteBans"
        icon-url: ""
      fields:
        - name: "Player"
          value: "%player%"
          inline: true
        - name: "<Actioned> by"
          value: "%executor%"
          inline: true
        - name: "Reason"
          value: "%reason%"
          inline: false
        - name: "Server"
          value: "%server_origin%"
          inline: true
        - name: "Punishment ID"
          value: "#%id%"
          inline: true
```

Verify the config key matches the LiteBans type string exactly before proceeding.

### Step 3 — Update `entryRemoved` switch (removal events only)

Skip this step if the new event is punishment-applied only.

File: `core/src/main/java/dev/colocated/litebansdiscordbridge/listener/LiteBansListener.java`

Locate the `switch` block inside `entryRemoved` and add a new `case`:
```java
switch (type.toLowerCase()) {
    case "ban":    eventType = "unban";   break;
    case "mute":   eventType = "unmute";  break;
    case "ipban":  eventType = "unipban"; break;  // ← add this
    default: return;
}
```

The left side of each case is the original punishment type string; the right side is the config key for the removal event.

Verify the new `case` value and mapped `eventType` string before building.

### Step 4 — Build and verify

```bash
mvn clean package
```

Expect three JARs:
- `bukkit/target/LiteBansDiscordBridge-bukkit-{version}.jar`
- `bungeecord/target/LiteBansDiscordBridge-bungeecord-{version}.jar`
- `velocity/target/LiteBansDiscordBridge-velocity-{version}.jar`

## Examples

**User says:** "Add support for IP bans and IP unbans"

**Actions taken:**
1. LiteBans type strings: `ipban` (applied) and `ipban` (removed, triggers `entryRemoved`)
2. Add `ipban:` block to `config.yml` under `events:`, copied from `ban:` block, title `"Player IP Banned"`, color `"#CC0000"`
3. Add `unipmute:` block — wait, this is IP *ban* removal. Add `unipban:` block to `config.yml`, copied from `unban:`, title `"Player IP Unbanned"`
4. In `entryRemoved` switch: add `case "ipban": eventType = "unipban"; break;`
5. `mvn clean package` — build succeeds

**Result:** `events.ipban` fires on IP ban applied; `events.unipban` fires when the IP ban is removed.

## Common Issues

- **Event fires but config block is silently skipped:** `eventConfig.virtual()` returns true. The config key doesn't match `entry.getType()`. Add a `platform.logWarn(...)` temporarily or check LiteBans source for the exact type string.
- **Removal event never fires:** The source type string is not in the `entryRemoved` switch. Add the missing `case` in `LiteBansListener.java:entryRemoved`.
- **`getBoolean(false)` always returns false:** `enabled:` is missing from the config block. Every event block must have `enabled: true`.
- **Build fails with `NoSuchMethodError` for SnakeYAML:** SnakeYAML version conflict. Ensure root `pom.xml` `<dependencyManagement>` pins `org.yaml:snakeyaml:1.33`.
- **Webhook not sent, no warning logged:** Webhook URL resolves to null. Either set the global `webhook-url` or add `webhook-url:` inside the event block; it must start with `https://`.