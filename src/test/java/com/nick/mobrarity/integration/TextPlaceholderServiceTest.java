package com.nick.mobrarity.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class TextPlaceholderServiceTest {
    @Test
    void replacesBuiltInValues() {
        TextPlaceholderService service = new TextPlaceholderService(false);

        String text = service.replaceBuiltIns("<aqua>[Lv %mobrarity_level%] %mobrarity_variant%</aqua>", Map.of(
                "mobrarity_level", "12",
                "mobrarity_variant", "Rare Sheep"
        ));

        assertThat(text).isEqualTo("<aqua>[Lv 12] Rare Sheep</aqua>");
    }

    @Test
    void exposesPlaceholderApiAvailability() {
        assertThat(new TextPlaceholderService(true).placeholderApiAvailable()).isTrue();
        assertThat(new TextPlaceholderService(false).placeholderApiAvailable()).isFalse();
    }

    @Test
    void replacesNullValuesWithEmptyString() {
        TextPlaceholderService service = new TextPlaceholderService(false);
        Map<String, String> values = new HashMap<>();
        values.put("mobrarity_variant", null);

        String text = service.replaceBuiltIns("Variant: %mobrarity_variant%", values);

        assertThat(text).isEqualTo("Variant: ");
    }
}
