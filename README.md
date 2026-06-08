# MobRarity

MobRarity is a Paper plugin foundation for configurable mob rarities, named variants, spawn-time tagging, optional mob level calculation, and command/listener shells for future effects and integrations.

## Build

```powershell
.\gradlew.bat build
```

The plugin jar is written to `build/libs/MobRarity-1.0.0-SNAPSHOT.jar`.

## Smoke Test Server

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\start-smoke-server.ps1
```

The script builds the plugin, downloads the latest Paper server jar from PaperMC, creates `.smoke-server`, installs `plugins/MobRarity.jar`, accepts the local test EULA, and starts the server with `nogui`.

Use `-PaperVersion <version>` to pin a Paper version, `-ServerDir <path>` to choose another test server folder, or `-SkipBuild` after a successful local build.

## Runtime

- Paper 26.1+.
- Java 25.
- Optional: LandClaims, VaultUnlocked, PlaceholderAPI.

## Admin Commands

Base command: `/mobrarity` with aliases `/mr` and `/mobr`.

- `/mobrarity reload` reloads `tiers.yml` and `mobs.yml` without restarting the server.
- `/mobrarity inspect` inspects the living mob in the player's crosshair.
- `/mobrarity set <tier> <variant> [level]` assigns configured MobRarity data to the targeted living mob.
- `/mobrarity clear` removes MobRarity data from the targeted living mob.
- `/mobrarity spawn <entity> <tier> <variant> [level] [player]` spawns and tags a configured mob on the sender or named online player.

All admin commands have per-action permissions under `mobrarity.<action>`, with `mobrarity.admin` granting the full set.

## Effect Actions

Configured triggers currently support:

- `item_drop` with `material` and numeric or ranged `amount`, such as `2-8`.
- `potion_effect` with `effect`, `duration-ticks`, `amplifier`, and optional `target: player|mob`.
- `currency_drop` with `amount`, paid through VaultUnlocked/Vault economy when available.
- `command` with `command` and optional `as: console|player`; `%player%` and `%entity_type%` placeholders are replaced.
- `hostile_target`, which makes a Bukkit `Mob` target the triggering player when the entity supports targeting.

`on_shear`, `on_aura_tick`, `on_damage`, `on_tame`, `on_breed`, `on_interact`, and player-caused `on_death` triggers are wired now.
