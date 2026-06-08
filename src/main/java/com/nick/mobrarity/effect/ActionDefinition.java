package com.nick.mobrarity.effect;

import java.util.Map;

public record ActionDefinition(String type, Map<String, Object> values) {
    public ActionDefinition {
        values = Map.copyOf(values);
    }
}
