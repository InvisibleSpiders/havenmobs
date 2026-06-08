# MobRarity

MobRarity is a Paper plugin foundation for configurable mob rarities, named variants, spawn-time tagging, optional mob level calculation, and command/listener shells for future effects and integrations.

## Build

```powershell
.\gradlew.bat build
```

The plugin jar is written to `build/libs/MobRarity-1.0.0-SNAPSHOT.jar`.

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
