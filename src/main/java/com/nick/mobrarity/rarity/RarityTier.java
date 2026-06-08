package com.nick.mobrarity.rarity;

import com.nick.mobrarity.effect.TriggerDefinition;
import java.util.Map;

public record RarityTier(String key, double weight, String displayName, Map<String, TriggerDefinition> triggers) {
    public RarityTier {
        triggers = Map.copyOf(triggers);
    }
}
