package com.nick.mobrarity.rarity;

import com.nick.mobrarity.effect.TriggerDefinition;
import com.nick.mobrarity.stat.StatModifier;
import java.util.Map;

public record MobVariant(
        String key,
        String tierKey,
        double weight,
        String nametag,
        Map<String, StatModifier> stats,
        Map<String, TriggerDefinition> triggers) {
    public MobVariant {
        stats = Map.copyOf(stats);
        triggers = Map.copyOf(triggers);
    }

    public MobVariant(
            String key,
            String tierKey,
            double weight,
            String nametag,
            Map<String, TriggerDefinition> triggers) {
        this(key, tierKey, weight, nametag, Map.of(), triggers);
    }
}
