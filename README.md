# MobRarity

MobRarity is a Paper plugin foundation for configurable mob rarities, named variants,
spawn-time tagging, mob levels, effects, and integrations.

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

- `/mobrarity reload`: requires `mobrarity.reload`; reloads rarity config files.
- `/mobrarity validate`: requires `mobrarity.validate`; checks config without applying changes.
- `/mobrarity list tiers`: requires `mobrarity.list`; lists configured global rarity tiers.
- `/mobrarity list variants`: requires `mobrarity.list`; lists configured mob variant keys.
- `/mobrarity list mobs`: requires `mobrarity.list`; lists mob types with MobRarity profiles.
- `/mobrarity inspect`: requires `mobrarity.inspect`; inspects the targeted living mob.
- `/mobrarity debug`: requires `mobrarity.debug`; reports targeted mob data, configured variants, tier weights, and spawn source settings.
- `/mobrarity set TIER VARIANT [level]`: requires `mobrarity.set`; tags the targeted mob.
- `/mobrarity clear`: requires `mobrarity.clear`; clears data from the targeted mob.
- `/mobrarity spawn ENTITY TIER VARIANT [level] [player]`: requires `mobrarity.spawn`.

`mobrarity.admin` grants all admin command permissions and the claim-check bypass permission.

## Permission Reference

- `mobrarity.admin`: default `op`; parent permission for admin commands and bypasses.
- `mobrarity.reload`: default `op`; allows `/mobrarity reload`.
- `mobrarity.validate`: default `op`; allows `/mobrarity validate`.
- `mobrarity.list`: default `op`; allows all `/mobrarity list` categories.
- `mobrarity.inspect`: default `op`; allows `/mobrarity inspect`.
- `mobrarity.debug`: default `op`; allows `/mobrarity debug`.
- `mobrarity.set`: default `op`; allows `/mobrarity set`.
- `mobrarity.clear`: default `op`; allows `/mobrarity clear`.
- `mobrarity.spawn`: default `op`; allows `/mobrarity spawn`.
- `mobrarity.bypass.claim-check`: default `op`; bypasses claim checks for effects.

## Effect Actions

Configured triggers currently support:

- `item_drop` with `material` and numeric or ranged `amount`, such as `2-8`.
- `potion_effect` with `effect`, `duration-ticks`, `amplifier`, and optional `target`.
- `currency_drop` with `amount`, paid through VaultUnlocked or Vault.
- `console_command` and `player_command` with `command`.
- `xp_drop` with `amount`.
- `heal` and `damage` with `amount` and optional `target`.
- `knockback` with `strength`, optional `y`, and optional `target`.
- `lightning_effect`, which shows lightning without dealing lightning damage.
- `hostile_target`, which makes a Bukkit `Mob` target the triggering player.

## Placeholders

MobRarity text supports built-in placeholders before MiniMessage parsing.
PlaceholderAPI exposes the same values for the player's targeted rarity mob.

- `%mobrarity_tier%`: tier display text, such as `Rare`.
- `%mobrarity_tier_key%`: raw tier key, such as `rare`.
- `%mobrarity_variant%`: variant display text, such as `Toxic Sheep`.
- `%mobrarity_variant_key%`: raw variant key, such as `toxic_sheep`.
- `%mobrarity_level%`: mob level.
- `%mobrarity_entity%`: mob type, such as `Sheep`.

## Stat Scaling

Tier and variant stats stack when a mob receives MobRarity data.

- Supported stats: `max-health`, `attack-damage`, `movement-speed`, `armor`.
- Also supported: `armor-toughness`, `knockback-resistance`, `follow-range`.
- Each stat can use `add`, `multiply`, and `per-level`.
- Changing `max-health` also heals the mob to its new maximum health.
- Original stat baselines are stored on the mob so repeated admin assignments do not compound scaling.
- `/mobrarity clear` removes rarity data and restores stored stat baselines.

Supported triggers include `on_shear`, `on_aura_tick`, `on_damage`, `on_tame`,
`on_breed`, `on_interact`, and player-caused `on_death`.

## Claim Protection

When LandClaims exposes `LandClaimsApi`, MobRarity checks claim access before effects run.

- Any action can set `claim-action` in `mobs.yml` to choose the LandClaims flag key.
- `hostile_target` defaults to `mob_griefing`.
- Other actions default to their action type.
- If LandClaims is not installed, the current fallback policy allows effects to run.
