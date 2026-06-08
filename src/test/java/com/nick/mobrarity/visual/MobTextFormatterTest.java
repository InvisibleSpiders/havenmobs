package com.nick.mobrarity.visual;

import static org.assertj.core.api.Assertions.assertThat;

import com.nick.mobrarity.integration.TextPlaceholderService;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class MobTextFormatterTest {
    @Test
    void formatsBuiltInValuesBeforeMiniMessage() {
        MobTextFormatter formatter = new MobTextFormatter(new TextPlaceholderService(false));

        String formatted = formatter.formatPlain("[Lv %mobrarity_level%] %mobrarity_variant%", Map.of(
                "mobrarity_level", "7",
                "mobrarity_variant", "Toxic Sheep"
        ));

        assertThat(formatted).isEqualTo("[Lv 7] Toxic Sheep");
    }
}
