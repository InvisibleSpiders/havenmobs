package com.nick.mobrarity.config;

import com.nick.mobrarity.rarity.MobProfile;
import com.nick.mobrarity.rarity.RarityTier;
import java.util.Map;
import org.bukkit.entity.EntityType;

public record ConfigSnapshot(Map<String, RarityTier> tiers, Map<EntityType, MobProfile> mobProfiles) {
    public ConfigSnapshot {
        tiers = Map.copyOf(tiers);
        mobProfiles = Map.copyOf(mobProfiles);
    }
}
