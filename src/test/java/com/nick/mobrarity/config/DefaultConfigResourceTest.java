package com.nick.mobrarity.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

final class DefaultConfigResourceTest {
    @Test
    void defaultYamlFilesExistAndAreCommented() throws Exception {
        assertCommented("config.yml", "leveling:");
        assertCommented("tiers.yml", "tiers:");
        assertCommented("mobs.yml", "mobs:");
    }

    private static void assertCommented(String resourceName, String requiredSection) throws Exception {
        try (InputStream stream = DefaultConfigResourceTest.class.getClassLoader().getResourceAsStream(resourceName)) {
            assertThat(stream).as(resourceName).isNotNull();
            String text = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(text).contains(requiredSection);
            assertThat(text.lines().filter(line -> line.stripLeading().startsWith("#")).count())
                    .as(resourceName + " comment count")
                    .isGreaterThanOrEqualTo(8);
        }
    }
}
