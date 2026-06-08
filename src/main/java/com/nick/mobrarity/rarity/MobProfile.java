package com.nick.mobrarity.rarity;

import java.util.Map;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

public record MobProfile(Map<SpawnReason, Boolean> spawnSources, Map<String, MobVariant> variants) {
    public MobProfile {
        spawnSources = Map.copyOf(spawnSources);
        variants = Map.copyOf(variants);
    }

    public boolean allows(SpawnReason reason) {
        return spawnSources.getOrDefault(reason, false);
    }
}
