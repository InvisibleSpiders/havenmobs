package com.nick.mobrarity.rarity;

import com.nick.mobrarity.effect.TriggerDefinition;
import com.nick.mobrarity.stat.StatModifier;
import java.util.Map;

public record RarityTier(
        String key,
        double weight,
        String displayName,
        Map<String, StatModifier> stats,
        Map<String, TriggerDefinition> triggers) {
    public RarityTier {
        stats = Map.copyOf(stats);
        triggers = Map.copyOf(triggers);
    }

    public RarityTier(String key, double weight, String displayName, Map<String, TriggerDefinition> triggers) {
        this(key, weight, displayName, Map.of(), triggers);
    }
}
