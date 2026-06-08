package com.nick.mobrarity.rarity;

import com.nick.mobrarity.effect.TriggerDefinition;
import java.util.Map;

public record MobVariant(String key, String tierKey, double weight, String nametag, Map<String, TriggerDefinition> triggers) {
    public MobVariant {
        triggers = Map.copyOf(triggers);
    }
}
