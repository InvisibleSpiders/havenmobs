package com.nick.mobrarity.integration;

import java.util.Map;
import java.util.Objects;

public final class TextPlaceholderService {
    private final boolean placeholderApiAvailable;

    public TextPlaceholderService(boolean placeholderApiAvailable) {
        this.placeholderApiAvailable = placeholderApiAvailable;
    }

    public String replaceBuiltIns(String input, Map<String, String> values) {
        if (input == null || values == null || values.isEmpty()) {
            return input;
        }

        String result = input;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace("%" + entry.getKey() + "%", Objects.toString(entry.getValue(), ""));
        }
        return result;
    }

    public boolean placeholderApiAvailable() {
        return placeholderApiAvailable;
    }
}
