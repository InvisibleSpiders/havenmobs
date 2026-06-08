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

## Commands And Permissions

Base command: `/mobrarity` with aliases `/mr` and `/mobr`.

| Command | Permission | Description |
| --- | --- | --- |
| `/mobrarity reload` | `mobrarity.reload` | Reloads `tiers.yml` and `mobs.yml` without restarting the server. |
| `/mobrarity validate` | `mobrarity.validate` | Parses `tiers.yml` and `mobs.yml` and reports whether they are valid without applying changes. |
| `/mobrarity list tiers` | `mobrarity.list` | Lists configured global rarity tiers. |
| `/mobrarity list variants` | `mobrarity.list` | Lists configured mob variant keys. |
| `/mobrarity list mobs` | `mobrarity.list` | Lists mob types with MobRarity profiles. |
| `/mobrarity inspect` | `mobrarity.inspect` | Inspects the living mob in the player's crosshair and reports its MobRarity data. |
| `/mobrarity set &lt;tier&gt; &lt;variant&gt; [level]` | `mobrarity.set` | Assigns configured MobRarity data to the targeted living mob. |
| `/mobrarity clear` | `mobrarity.clear` | Removes MobRarity data from the targeted living mob. |
| `/mobrarity spawn &lt;entity&gt; &lt;tier&gt; &lt;variant&gt; [level] [player]` | `mobrarity.spawn` | Spawns and tags a configured mob on the sender or named online player. |

`mobrarity.admin` grants all admin command permissions and the claim-check bypass permission.

## Permission Reference

| Permission | Default | Description |
| --- | --- | --- |
| `mobrarity.admin` | `op` | Parent permission for every MobRarity admin command and bypass permission. |
| `mobrarity.reload` | `op` | Allows `/mobrarity reload`. |
| `mobrarity.validate` | `op` | Allows `/mobrarity validate`. |
| `mobrarity.list` | `op` | Allows `/mobrarity list tiers`, `/mobrarity list variants`, and `/mobrarity list mobs`. |
| `mobrarity.inspect` | `op` | Allows `/mobrarity inspect`. |
| `mobrarity.set` | `op` | Allows `/mobrarity set`. |
| `mobrarity.clear` | `op` | Allows `/mobrarity clear`. |
| `mobrarity.spawn` | `op` | Allows `/mobrarity spawn`. |
| `mobrarity.bypass.claim-check` | `op` | Allows MobRarity effects to run for the player even when a claim protection check would normally deny them. |

## Effect Actions

Configured triggers currently support:

- `item_drop` with `material` and numeric or ranged `amount`, such as `2-8`.
- `potion_effect` with `effect`, `duration-ticks`, `amplifier`, and optional `target: player|mob`.
- `currency_drop` with `amount`, paid through VaultUnlocked/Vault economy when available.
- `command` with `command` and optional `as: console|player`; `%player%` and `%entity_type%` placeholders are replaced.
- `hostile_target`, which makes a Bukkit `Mob` target the triggering player when the entity supports targeting.

`on_shear`, `on_aura_tick`, `on_damage`, `on_tame`, `on_breed`, `on_interact`, and player-caused `on_death` triggers are wired now.

## Claim Protection

When LandClaims is installed and exposes `LandClaimsApi`, MobRarity checks claim access before effect actions run.

- Any action can set `claim-action` in `mobs.yml` to choose the LandClaims action or flag key checked before the effect runs.
- `hostile_target` defaults to `mob_griefing`.
- Other actions default to their action type, such as `item_drop`, `currency_drop`, or `potion_effect`.
- If LandClaims is not installed, the current fallback policy allows effects to run.
