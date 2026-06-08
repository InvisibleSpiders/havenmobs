package com.nick.mobrarity;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class RarityMobPluginTest {
    @TempDir
    Path tempDir;

    @Test
    void pluginClassExists() {
        assertThat(RarityMobPlugin.class.getName()).isEqualTo("com.nick.mobrarity.RarityMobPlugin");
    }

    @Test
    void loadsRuntimeConfigFromDataFolderFiles() throws Exception {
        Files.writeString(tempDir.resolve("tiers.yml"), """
                tiers:
                  test_tier:
                    weight: 1.0
                    display-name: Test Tier
                """);
        Files.writeString(tempDir.resolve("mobs.yml"), """
                mobs:
                  ZOMBIE:
                    spawn-sources:
                      NATURAL: true
                    variants:
                      test_zombie:
                        tier: test_tier
                        weight: 1.0
                """);

        var snapshot = RarityMobPlugin.loadRuntimeConfig(tempDir);

        assertThat(snapshot.mobProfiles()).containsOnlyKeys(EntityType.ZOMBIE);
        assertThat(snapshot.mobProfiles().get(EntityType.ZOMBIE).variants()).containsOnlyKeys("test_zombie");
    }
}
