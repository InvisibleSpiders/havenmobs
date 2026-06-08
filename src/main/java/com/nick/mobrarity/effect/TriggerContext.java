package com.nick.mobrarity.effect;

import java.util.Map;

public record TriggerContext(Map<String, Object> values) {
    private static final TriggerContext EMPTY = new TriggerContext(Map.of());

    public TriggerContext {
        values = Map.copyOf(values);
    }

    public static TriggerContext empty() {
        return EMPTY;
    }
}
