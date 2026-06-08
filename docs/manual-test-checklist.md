# Manual Test Checklist

## Current Foundation Checks

- Start a Paper 26.1+ server with Java 25.
- Install `MobRarity-1.0.0-SNAPSHOT.jar` only.
- Confirm the plugin loads without LandClaims, VaultUnlocked, or PlaceholderAPI.
- Confirm commented `config.yml`, `tiers.yml`, and `mobs.yml` are created.
- Run `/mobrarity` and confirm it prints `/mobrarity reload|inspect|set|spawn|clear`; tab-complete base subcommands.
- Spawn natural or configured sheep if practical and confirm the plugin does not error while assigning rarity metadata.
- Spawn mobs below Y:64 and confirm there are no startup or runtime errors from depth-capable level calculation if practical.

## Future Wiring Checks

- PlaceholderAPI nametag rendering.
- VaultUnlocked rewards.
- LandClaims blocking Toxic Sheep aura and Rare Sheep shearing rewards.
- `/mobrarity spawn` and inspect behavior.
- Rare Sheep shearing drops.
- Lava or natural death reward suppression.
- Distance-from-spawn level scaling once horizontal distance is passed into `MobLevelService`.
