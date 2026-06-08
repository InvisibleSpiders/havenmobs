package com.nick.mobrarity.effect;

import java.util.List;

public record TriggerDefinition(String key, double chance, long intervalTicks, double radius, List<ActionDefinition> actions) {
    public TriggerDefinition {
        actions = List.copyOf(actions);
    }
}
