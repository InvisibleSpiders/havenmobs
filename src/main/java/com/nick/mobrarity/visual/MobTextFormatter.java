package com.nick.mobrarity.visual;

import com.nick.mobrarity.integration.TextPlaceholderService;
import java.util.Map;
import java.util.Objects;

public final class MobTextFormatter {
    private final TextPlaceholderService textPlaceholderService;

    public MobTextFormatter(TextPlaceholderService textPlaceholderService) {
        this.textPlaceholderService = Objects.requireNonNull(textPlaceholderService, "textPlaceholderService");
    }

    public String formatPlain(String template, Map<String, String> values) {
        return textPlaceholderService.replaceBuiltIns(template, values);
    }
}
