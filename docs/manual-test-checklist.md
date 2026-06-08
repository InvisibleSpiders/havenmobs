# Manual Test Checklist

## Current Foundation Checks

- Start a Paper 26.1+ server with Java 25 by running `powershell -NoProfile -ExecutionPolicy Bypass -File ./scripts/start-smoke-server.ps1`.
- Confirm the script installs `plugins/MobRarity.jar` in `.smoke-server`.
- Confirm the plugin loads without LandClaims, VaultUnlocked, or PlaceholderAPI.
- Confirm commented `config.yml`, `tiers.yml`, and `mobs.yml` are created.
- Run `/mobrarity` and confirm it prints `/mobrarity reload|inspect|set|spawn|clear`; tab-complete base subcommands.
- Run `/mobrarity reload` and confirm it reports `Reloaded MobRarity config.`
- Run `/mobrarity spawn SHEEP rare rare_sheep 3` and confirm a sheep spawns on the player.
- Look at that sheep and run `/mobrarity inspect`; confirm it reports `SHEEP rare/rare_sheep level 3`.
- Look at that sheep and run `/mobrarity set rare toxic_sheep 4`; confirm it reports the assigned tier, variant, and level.
- Look at that sheep and run `/mobrarity clear`; confirm it removes MobRarity data.
- Spawn natural or configured sheep if practical and confirm the plugin does not error while assigning rarity metadata.
- Spawn mobs below Y:64 and confirm there are no startup or runtime errors from depth-capable level calculation if practical.

## Future Wiring Checks

- PlaceholderAPI nametag rendering.
- VaultUnlocked rewards.
- LandClaims blocking Toxic Sheep aura and Rare Sheep shearing rewards.
- Rare Sheep shearing drops.
- Lava or natural death reward suppression.
- Distance-from-spawn level scaling once horizontal distance is passed into `MobLevelService`.
