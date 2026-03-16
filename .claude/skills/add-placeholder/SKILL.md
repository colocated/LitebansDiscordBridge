---
name: add-placeholder
description: Extends PlaceholderReplacer with a new %placeholder% backed by a litebans.api.Entry field. Use when user says 'add placeholder', 'new variable', 'support %foo%', or references an Entry getter not yet in PlaceholderReplacer.replace(). Do NOT use for config.yml embed field changes or DiscordEmbed modifications.
---
# add-placeholder

## Critical

- **Only edit one file:** `core/src/main/java/dev/colocated/litebansdiscordbridge/util/PlaceholderReplacer.java`
- Never break the null-guard at the top of `replace()` — `if (text == null || entry == null) return text;` must stay first.
- All fallback strings must be non-null literals (`"Unknown"`, `"Never"`, `"No reason specified"`, etc.) — never `null`.
- Do not add imports that aren't already present unless a new helper method requires one.

## Instructions

1. **Read the file** `core/src/main/java/dev/colocated/litebansdiscordbridge/util/PlaceholderReplacer.java` in full.
   Verify `replace(String text, Entry entry, PlatformAdapter platform)` is the method you will modify before proceeding.

2. **Determine which pattern fits the new placeholder:**
   - **Simple** (direct Entry getter, cheap): add a `.replace("%name%", entry.getX() != null ? entry.getX() : "Fallback")` line inside the existing chained block (lines ~51–67).
   - **Expensive or needs helper** (e.g., async lookup, formatting, parsing): add a `text.contains("%name%")` guard block after the chained block, calling a new `private static` helper method.

3. **Simple placeholder — add to the chained `.replace()` block:**
   ```java
   // inside the text = text\n    .replace(...) chain
   .replace("%new_field%", entry.getNewField() != null ? entry.getNewField() : "Unknown")
   ```
   Boolean fields use `String.valueOf(entry.isSomething())` with no null check (primitives).
   Long/numeric fields use `String.valueOf(entry.getId())` with no null check.

4. **Expensive placeholder — add a guard block after the chain:**
   ```java
   if (text.contains("%new_field%")) {
       text = text.replace("%new_field%", formatNewField(entry.getNewField()));
   }
   ```
   Then add the private static helper below `formatDate`:
   ```java
   private static String formatNewField(SomeType value) {
       if (value == null) return "Unknown";
       // transform...
       return result;
   }
   ```

5. **Aliases** — if the placeholder should have an alias (e.g., `%player%` and `%player_name%` both resolve the same value), compute the value once and chain both replacements:
   ```java
   if (text.contains("%new_field%") || text.contains("%new_field_alias%")) {
       String val = computeVal(entry);
       text = text.replace("%new_field%", val)
                  .replace("%new_field_alias%", val);
   }
   ```

6. **Verify** the method still compiles by running:
   ```bash
   mvn clean package -pl core -am -q
   ```
   Confirm `BUILD SUCCESS` before considering the task done.

## Examples

**User says:** "Add a `%country%` placeholder from `entry.getCountry()`"

**Actions taken:**
- Identified `getCountry()` returns `String` (nullable) — simple pattern.
- Added to the chained block:
  ```java
  .replace("%country%", entry.getCountry() != null ? entry.getCountry() : "Unknown")
  ```
- Ran `mvn clean package -pl core -am -q` → `BUILD SUCCESS`.

**Result:** Users can now use `%country%` in any `config.yml` embed field string.

---

**User says:** "Add `%time_since_ban%` showing how long ago the ban was issued"

**Actions taken:**
- Requires computation — expensive pattern.
- Added guard block in `replace()`:
  ```java
  if (text.contains("%time_since_ban%")) {
      text = text.replace("%time_since_ban%", formatTimeSince(entry.getDateStart()));
  }
  ```
- Added private helper after `formatDate`:
  ```java
  private static String formatTimeSince(long timestamp) {
      if (timestamp <= 0) return "Unknown";
      return formatDuration(System.currentTimeMillis() - timestamp) + " ago";
  }
  ```
- Ran `mvn clean package -pl core -am -q` → `BUILD SUCCESS`.

## Common Issues

- **`cannot find symbol: method getX()` on `Entry`** — `getX()` does not exist in `litebans.api.Entry`. Check the LiteBans API jar for actual method names. The `Entry` interface is in `litebans.api.Entry` (shaded at build time from the `litebans-api` dependency in `core/pom.xml`).
- **`NullPointerException` at runtime on `String.valueOf(entry.getX())`** — `String.valueOf(null)` returns the literal string `"null"`. Use a ternary instead: `entry.getX() != null ? entry.getX() : "Unknown"`.
- **`BUILD FAILURE: package litebans.api does not exist`** — the LiteBans API jar must be installed locally. See `core/pom.xml` for the `system`-scoped dependency path.
- **Placeholder not replaced in output** — the placeholder string in `config.yml` must match exactly, including percent signs and underscores. Verify `text.contains("%exact_name%")` matches what is in the config.