# Mob Rarity Plugin Design

## Summary

Build a Paper plugin for configurable mob rarities, named variants, event-driven effects, visual presentation, optional mob leveling, and soft integrations with LandClaims, VaultUnlocked, and PlaceholderAPI.

The plugin targets the latest Paper line, currently Paper 26.1+ with Java 25, while avoiding NMS and internal server classes so newer Paper versions are less likely to break plugin loading. Paper's project setup documentation recommends Gradle with Kotlin DSL and Java toolchains, so the project will use a Gradle Java build and compile against the Paper API.

## Goals

- Assign configurable rarity tiers and named variants to mobs when they spawn.
- Support global tiers with per-entity overrides.
- Support admin tools to assign rarity, spawn a mob with a specific rarity or variant, and spawn a rarity mob at or near a player.
- Allow or deny rarity assignment by spawn source, including natural spawns, spawners, spawn eggs, breeding, commands, and plugin-created entities.
- Trigger effects only from eligible player-driven actions unless a trigger is explicitly passive, such as an aura tick.
- Respect claimed land through the future LandClaims API integration.
- Support item drops, economy rewards, commands, potion effects, stat or attribute changes, particles, sounds, messages, hostility targeting, extra damage, and healing.
- Use MiniMessage for all user-visible in-game text.
- Optionally resolve PlaceholderAPI placeholders before MiniMessage rendering.
- Optionally calculate mob levels from distance to an origin and depth below a configured Y coordinate.
- Keep integrations optional so the plugin still loads if LandClaims, VaultUnlocked, or PlaceholderAPI are missing.
- Ship default YAML files with practical comments that explain each section, common values, and what gameplay behavior the setting controls.

## Non-Goals

- Do not use NMS, reflection into server internals, or version-specific CraftBukkit classes.
- Do not implement a scripting language in the first version.
- Do not require LandClaims to be complete before this plugin can be built.
- Do not add ExcellentEconomy in the first version; leave an adapter slot for later multi-currency support.
- Do not make naturally caused damage, such as lava, cactus, falling, or suffocation, trigger player-reward effects unless the configuration explicitly opts into a non-player trigger.

## Architecture

### Main Components

- `RarityMobPlugin`: plugin lifecycle, dependency discovery, command registration, listener registration, scheduler startup, reload flow.
- `ConfigService`: loads, validates, and exposes YAML configuration files.
- `RarityRegistry`: stores tiers, variants, per-mob overrides, spawn-source rules, trigger definitions, conditions, and actions.
- `MobTagService`: persists assigned tier, variant, level, and metadata on mobs using `PersistentDataContainer`.
- `SpawnRarityService`: decides whether a spawning mob receives a tier or variant.
- `MobLevelService`: calculates mob level from world-specific horizontal distance and optional Y-depth rules.
- `EffectEngine`: evaluates trigger conditions and executes effect actions.
- `ProtectionService`: gates triggers through LandClaims when available and applies configured fallback behavior when unavailable.
- `EconomyService`: deposits VaultUnlocked rewards when available and logs a clear warning when economy rewards are configured but unavailable.
- `VisualService`: handles nametags, titles, targeted display, distance display, particles, MiniMessage rendering, and optional PlaceholderAPI resolution.
- `CommandService`: exposes admin and reload commands.

### Data Flow

When a mob spawns, the plugin checks whether the entity type and spawn reason are enabled. If enabled, the plugin calculates the mob level, adjusts configured rarity weights using that level, chooses an eligible tier or variant, stores the result on the mob, applies stat scaling, applies configured visuals, and runs `on_spawn`.

When a player damages, kills, shears, tames, breeds, or interacts with a tagged mob, the event listener confirms the event is eligible, asks `ProtectionService` whether the player may perform that action at the mob location, then passes the trigger to `EffectEngine`.

Passive triggers, such as toxic aura effects, run on scheduled intervals. They must still check claim permissions before affecting a player. For example, a Toxic Sheep should not poison a player inside a claim unless LandClaims allows the configured action key for that situation.

## Configuration Model

The plugin uses global tiers with per-mob overrides. Tiers define shared rarity behavior, while variants define named special mobs such as `Toxic Sheep` or `Rare Sheep`.

Default configuration files should be commented enough that a server owner can safely edit them without reading source code. Comments should explain what each major section does, which values are common, and which settings affect gameplay balance or performance. The implementation should preserve bundled defaults through `saveResource` on first startup; it does not need to preserve comments when administrators edit and reload their own files.

Example shape:

```yaml
tiers:
  common:
    weight: 100
    display-name: "<gray>Common</gray>"
  rare:
    weight: 8
    display-name: "<aqua>Rare</aqua>"
    visuals:
      particles:
        enabled: true
        particle: HAPPY_VILLAGER
        interval-ticks: 40

mobs:
  SHEEP:
    spawn-sources:
      NATURAL: true
      SPAWNER: false
      SPAWN_EGG: true
      BREEDING: true
      COMMAND: true
      PLUGIN: true
    variants:
      toxic_sheep:
        tier: rare
        weight: 2
        display:
          nametag: "<green>Toxic Sheep</green>"
        triggers:
          on_aura_tick:
            interval-ticks: 40
            radius: 4
            actions:
              - type: potion_effect
                effect: POISON
                duration-ticks: 80
                amplifier: 0
      rare_sheep:
        tier: rare
        weight: 3
        display:
          nametag: "<aqua>Rare Sheep</aqua>"
        triggers:
          on_shear:
            chance: 0.25
            actions:
              - type: item_drop
                material: DIAMOND
                amount: 1
              - type: item_drop
                material: GOLD_NUGGET
                amount: "2-8"
```

## Trigger Model

Triggers are named event groups that can be declared at the tier or variant level. By default, variant triggers are added to tier triggers. If a variant trigger sets `mode: replace`, it replaces the tier trigger with the same name. If it sets `mode: append` or omits `mode`, it appends its conditions and actions after the tier trigger's conditions and actions.

Initial triggers:

- `on_spawn`: runs after a mob receives a tier or variant.
- `on_damage`: runs when a player damages the mob.
- `on_damaged_by_mob`: runs when a rarity mob damages a player.
- `on_death`: runs when the mob dies from eligible player-caused damage.
- `on_shear`: runs when a player shears a supported rarity mob.
- `on_tame`: runs when a player tames a rarity mob.
- `on_breed`: runs when breeding involves a rarity mob.
- `on_interact`: runs on configured player interactions.
- `on_aura_tick`: runs on a configured schedule for passive effects.
- `on_target`: runs when a player targets the mob, primarily for visuals and messages.

Each trigger may configure chance, cooldowns, radius, target selector, conditions, and actions.

## Effect Actions

The first version supports these action families:

- `item_drop`: drop configured materials, amounts, names, lore, enchantments, and chances.
- `economy_deposit`: deposit VaultUnlocked currency to the eligible player.
- `console_command`: run a command from console with placeholders.
- `player_command`: make the player run a command when allowed by config.
- `potion_effect`: apply or remove potion effects.
- `attribute_modifier`: adjust health, damage, armor, movement speed, knockback resistance, or similar Bukkit attributes.
- `particle`: spawn configured particles.
- `sound`: play configured sounds.
- `message`: send chat, actionbar, title, or subtitle messages.
- `set_target`: make the mob hostile toward the triggering player when supported by the entity type.
- `damage`: apply extra damage.
- `heal`: heal the mob or player.
- `xp_drop`: modify or add experience rewards.

Actions should fail safely. If an action references an invalid material, particle, sound, attribute, or potion effect, the config validator reports it during load and disables that action instead of crashing the server.

## Player-Caused Eligibility

Reward-like triggers should only run from eligible player activity. The plugin tracks the last player damage source for tagged mobs and uses that record for `on_death`.

Eligible sources include direct melee, projectiles shot by players, and tamed pets owned by a player when configured. Ineligible sources include falling, lava, fire, cactus, drowning, suffocation, entity cramming, lightning, explosions without a player source, and other natural damage.

This prevents accidental farms or environmental hazards from triggering rare loot or economy rewards.

## LandClaims Integration

LandClaims is a soft dependency. The mob rarity plugin will look for a registered `com.nick.landclaims.api.LandClaimsApi` service. If it exists, protection checks call:

```java
canInteract(player, location, actionKey)
```

Planned action keys:

- `entity_damage`
- `entity_shear`
- `entity_breed`
- `entity_tame`
- `entity_interact`
- `entity_kill`
- `player_damage_from_mob`
- `mob_aura_effect`

If LandClaims is missing or does not expose the service yet, the plugin follows a configurable fallback:

```yaml
integrations:
  landclaims:
    missing-service-policy: allow
```

Valid fallback values are `allow`, `deny-protected-effects`, and `deny-all-effects`. `allow` runs configured effects normally. `deny-protected-effects` blocks effects that require LandClaims action keys but still allows purely visual effects. `deny-all-effects` blocks all non-admin rarity triggers until LandClaims is available. The default is `allow` for easier development, with a clear startup warning when claim integration is unavailable.

## Economy Integration

VaultUnlocked is a soft dependency for the first version. Economy actions are skipped when VaultUnlocked is absent, and the plugin logs one startup warning if any enabled config section references economy rewards.

ExcellentEconomy is reserved for a later adapter that supports named currency IDs and multi-currency rewards.

## Visuals And Text

All user-facing strings use MiniMessage. Placeholder resolution happens before MiniMessage rendering.

Optional PlaceholderAPI support:

1. Replace plugin placeholders such as `%mobrarity_tier%`, `%mobrarity_variant%`, `%mobrarity_level%`, `%mobrarity_health%`, and `%mobrarity_max_health%`.
2. If PlaceholderAPI is present, resolve external placeholders for the relevant player.
3. Render the final text using MiniMessage.

Display modes:

- `disabled`: no nametag or title.
- `always`: nametag is always applied.
- `within-distance`: display only within configured distance.
- `targeted`: display when the player is aiming at the mob.

Particles are optional per tier or variant and support interval, radius, particle type, count, and conditions.

## Mob Leveling

Leveling is optional and calculated at spawn before rarity selection.

Horizontal component:

- Origin can be `world-spawn` or fixed coordinates per world.
- Distance from origin maps to levels through `blocks-per-level`.
- Each world can define `min-level`, `max-level`, and whether leveling is enabled.

Depth component:

- Above the configured Y threshold, only the horizontal component applies.
- Below the threshold, the plugin adds bonus levels or a multiplier.
- The bonus has a configurable cap.

Example:

```yaml
leveling:
  enabled: true
  worlds:
    world:
      origin: world-spawn
      blocks-per-level: 250
      min-level: 1
      max-level: 100
      depth-bonus:
        enabled: true
        starts-below-y: 64
        levels-per-y: 0.15
        max-bonus-levels: 25
      rarity-weight-modifiers:
        rare:
          per-level: 0.03
        epic:
          starts-at-level: 20
          per-level: 0.02
        legendary:
          starts-at-level: 50
          per-level: 0.01
```

Levels may scale health, damage, armor, speed, XP drops, item amounts, economy rewards, and command placeholders.

## Commands And Permissions

Initial commands:

- `/mobrarity reload`: reload config and report validation issues.
- `/mobrarity inspect`: inspect the targeted mob's tier, variant, level, and stored metadata.
- `/mobrarity set <tier|variant> [target]`: assign rarity to the targeted mob or entity selector.
- `/mobrarity spawn <entity> <tier|variant> [player|x y z world]`: spawn a mob with a specific tier or variant.
- `/mobrarity clear [target]`: remove rarity metadata from a mob.

Initial permissions:

- `mobrarity.admin`
- `mobrarity.reload`
- `mobrarity.inspect`
- `mobrarity.set`
- `mobrarity.spawn`
- `mobrarity.clear`
- `mobrarity.bypass.claim-check`

## Error Handling

Config reload should be safe. If the new config is invalid, the plugin keeps the last valid config and reports validation errors to the command sender and console.

Invalid actions are disabled with warnings instead of crashing the server. Missing optional integrations produce startup warnings only when related features are configured.

If a trigger execution throws unexpectedly, the plugin logs the trigger, entity type, tier, variant, and action type, then continues processing other entities and events.

## Testing Strategy

Unit tests should cover:

- YAML parsing and validation.
- Tier and variant registry behavior.
- Spawn-source allow/deny rules.
- Weighted rarity selection.
- Level calculation from distance and Y-depth.
- Level-adjusted rarity weights.
- Protection fallback policies.
- Placeholder replacement before MiniMessage rendering.
- Effect action validation.

Integration-style tests with mocked Bukkit/Paper APIs should cover:

- Player-caused damage tracking.
- `on_death` only firing for eligible player-caused kills.
- Shear, tame, breed, interact, and aura trigger routing.
- LandClaims service present, missing, allow, and deny behavior.
- VaultUnlocked missing behavior for economy actions.

Manual server verification should cover:

- Plugin loads on the target latest Paper server with Java 25.
- Plugin loads when LandClaims, VaultUnlocked, and PlaceholderAPI are absent.
- Admin spawn and set commands work.
- Rare Sheep shearing drops configured items only when allowed.
- Toxic Sheep aura respects claim checks.
- Targeted and distance-based display modes render correctly.

## Compatibility Notes

Paper documentation currently lists Java 25 as required for Paper 26.1+ and shows `paper-api:26.1.2.build.+` with Java toolchain 25 in the Gradle setup. The plugin should follow that baseline while avoiding internals to reduce future version breakage.

Sources:

- Paper requirements: https://docs.papermc.io/paper/getting-started/
- Paper Gradle setup: https://docs.papermc.io/paper/dev/project-setup/
