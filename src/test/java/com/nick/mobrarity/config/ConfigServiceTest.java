package com.nick.mobrarity.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nick.mobrarity.rarity.MobProfile;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ConfigServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void loadsDefaultSheepVariants() {
        ConfigService service = ConfigService.fromResources();

        MobProfile sheep = service.snapshot().mobProfiles().get(EntityType.SHEEP);

        assertThat(sheep).isNotNull();
        assertThat(sheep.variants()).containsKeys("toxic_sheep", "rare_sheep");
        assertThat(service.snapshot().tiers()).containsKeys("common", "rare");
    }

    @Test
    void loadsFromFilePaths() throws Exception {
        Path tiersPath = tempDir.resolve("tiers.yml");
        Path mobsPath = tempDir.resolve("mobs.yml");
        Files.writeString(tiersPath, """
                tiers:
                  rare:
                    weight: 1.0
                    display-name: Rare
                """);
        Files.writeString(mobsPath, """
                mobs:
                  SHEEP:
                    variants:
                      rare_sheep:
                        tier: rare
                        weight: 1.0
                """);

        ConfigService service = ConfigService.fromFiles(tiersPath, mobsPath);

        assertThat(service.snapshot().mobProfiles().get(EntityType.SHEEP).variants()).containsKey("rare_sheep");
        assertThat(service.snapshot().tiers()).containsKey("rare");
    }

    @Test
    void loadsVariantTriggersAndActions() {
        ConfigService service = ConfigService.fromResources();

        var rareSheep = service.snapshot().mobProfiles().get(EntityType.SHEEP).variants().get("rare_sheep");

        assertThat(rareSheep.triggers()).containsKey("on_shear");
        assertThat(rareSheep.triggers().get("on_shear").chance()).isEqualTo(0.25);
        assertThat(rareSheep.triggers().get("on_shear").actions()).hasSize(3);
        assertThat(rareSheep.triggers().get("on_shear").actions().get(0).type()).isEqualTo("sound");
        assertThat(rareSheep.triggers().get("on_shear").actions().get(1).type()).isEqualTo("item_drop");
        assertThat(rareSheep.triggers().get("on_shear").actions().get(1).values()).containsEntry("material", "DIAMOND");
    }

    @Test
    void loadsTierAndVariantStatModifiers() {
        ConfigService service = ConfigService.fromYaml(yaml("""
                tiers:
                  rare:
                    weight: 1.0
                    stats:
                      max-health:
                        add: 4.0
                        per-level: 0.25
                """), yaml("""
                mobs:
                  SHEEP:
                    variants:
                      toxic_sheep:
                        tier: rare
                        weight: 1.0
                        stats:
                          movement-speed:
                            multiply: 1.15
                """));

        var tierStats = service.snapshot().tiers().get("rare").stats();
        var variantStats = service.snapshot().mobProfiles()
                .get(EntityType.SHEEP)
                .variants()
                .get("toxic_sheep")
                .stats();

        assertThat(tierStats.get("max-health").add()).isEqualTo(4.0);
        assertThat(tierStats.get("max-health").perLevel()).isEqualTo(0.25);
        assertThat(variantStats.get("movement-speed").multiply()).isEqualTo(1.15);
    }

    @Test
    void reportsUnknownTierReference() {
        ValidationResult result = ConfigService.validateVariantTier("missing", java.util.Set.of("rare"));

        assertThat(result.valid()).isFalse();
        assertThat(result.messages()).contains("Unknown tier 'missing'.");
    }

    @Test
    void mapsDefaultSpawnSourceAliases() {
        ConfigService service = ConfigService.fromResources();

        MobProfile sheep = service.snapshot().mobProfiles().get(EntityType.SHEEP);

        assertThat(sheep.allows(SpawnReason.SPAWNER_EGG)).isTrue();
        assertThat(sheep.allows(SpawnReason.SPAWNER)).isFalse();
    }

    @Test
    void invalidEntityTypeThrowsConfigValidationException() {
        assertThatThrownBy(() -> ConfigService.fromYaml(yaml("""
                tiers:
                  rare:
                    weight: 1.0
                """), yaml("""
                mobs:
                  SHEEPY: {}
                """)))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining("Unknown entity type 'SHEEPY'")
                .hasMessageContaining("mobs.SHEEPY")
                .hasMessageContaining("mobs.yml");
    }

    @Test
    void malformedMobSectionThrows() {
        assertThatThrownBy(() -> ConfigService.fromYaml(yaml("""
                tiers:
                  rare:
                    weight: 1.0
                """), yaml("""
                mobs:
                  SHEEP: bad
                """)))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining("mobs.SHEEP")
                .hasMessageContaining("mobs.yml");
    }

    @Test
    void malformedVariantsSectionThrows() {
        assertThatThrownBy(() -> ConfigService.fromYaml(yaml("""
                tiers:
                  rare:
                    weight: 1.0
                """), yaml("""
                mobs:
                  SHEEP:
                    variants: bad
                """)))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining("mobs.SHEEP.variants")
                .hasMessageContaining("mobs.yml");
    }

    @Test
    void malformedTriggersSectionThrows() {
        assertThatThrownBy(() -> ConfigService.fromYaml(yaml("""
                tiers:
                  rare:
                    weight: 1.0
                """), yaml("""
                mobs:
                  SHEEP:
                    variants:
                      rare_sheep:
                        tier: rare
                        triggers: bad
                """)))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining("mobs.SHEEP.variants.rare_sheep.triggers")
                .hasMessageContaining("mobs.yml");
    }

    @Test
    void malformedTierEntryThrows() {
        assertThatThrownBy(() -> ConfigService.fromYaml(yaml("""
                tiers:
                  rare: 5
                """), yaml("""
                mobs: {}
                """)))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining("tiers.rare");
    }

    @Test
    void invalidTierKeyContainingDelimiterThrows() {
        assertThatThrownBy(() -> ConfigService.fromYaml(yaml("""
                tiers:
                  rare|bad:
                    weight: 1.0
                """), yaml("""
                mobs: {}
                """)))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining("tiers.rare|bad")
                .hasMessageContaining("tiers.yml");
    }

    @Test
    void invalidVariantKeyContainingDelimiterThrows() {
        assertThatThrownBy(() -> ConfigService.fromYaml(yaml("""
                tiers:
                  rare:
                    weight: 1.0
                """), yaml("""
                mobs:
                  SHEEP:
                    variants:
                      rare|sheep:
                        tier: rare
                        weight: 1.0
                """)))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining("mobs.SHEEP.variants.rare|sheep")
                .hasMessageContaining("mobs.yml");
    }

    @Test
    void invalidSpawnSourceThrows() {
        assertThatThrownBy(() -> ConfigService.fromYaml(yaml("""
                tiers:
                  rare:
                    weight: 1.0
                """), yaml("""
                mobs:
                  SHEEP:
                    spawn-sources:
                      BAD_SOURCE: true
                    variants: {}
                """)))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining("mobs.SHEEP.spawn-sources.BAD_SOURCE");
    }

    @Test
    void malformedSpawnSourcesSectionThrows() {
        assertThatThrownBy(() -> ConfigService.fromYaml(yaml("""
                tiers:
                  rare:
                    weight: 1.0
                """), yaml("""
                mobs:
                  SHEEP:
                    spawn-sources: bad
                    variants: {}
                """)))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining("mobs.SHEEP.spawn-sources")
                .hasMessageContaining("mobs.yml");
    }

    @Test
    void malformedActionsSectionThrows() {
        assertThatThrownBy(() -> ConfigService.fromYaml(yaml("""
                tiers:
                  rare:
                    weight: 1.0
                """), yaml("""
                mobs:
                  SHEEP:
                    variants:
                      rare_sheep:
                        tier: rare
                        triggers:
                          on_shear:
                            actions: bad
                """)))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining("mobs.SHEEP.variants.rare_sheep.triggers.on_shear.actions")
                .hasMessageContaining("mobs.yml");
    }

    @Test
    void nonMapActionEntryThrows() {
        assertThatThrownBy(() -> ConfigService.fromYaml(yaml("""
                tiers:
                  rare:
                    weight: 1.0
                """), yaml("""
                mobs:
                  SHEEP:
                    variants:
                      rare_sheep:
                        tier: rare
                        triggers:
                          on_shear:
                            actions:
                              - bad
                """)))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining("mobs.SHEEP.variants.rare_sheep.triggers.on_shear.actions[0]")
                .hasMessageContaining("mobs.yml");
    }

    @Test
    void missingActionTypeThrows() {
        assertThatThrownBy(() -> ConfigService.fromYaml(yaml("""
                tiers:
                  rare:
                    weight: 1.0
                """), yaml("""
                mobs:
                  SHEEP:
                    variants:
                      rare_sheep:
                        tier: rare
                        triggers:
                          on_shear:
                            actions:
                              - material: DIAMOND
                """)))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining("mobs.SHEEP.variants.rare_sheep.triggers.on_shear.actions[0]")
                .hasMessageContaining("type");
    }

    @Test
    void nullActionValueThrows() {
        assertThatThrownBy(() -> ConfigService.fromYaml(yaml("""
                tiers:
                  rare:
                    weight: 1.0
                """), yaml("""
                mobs:
                  SHEEP:
                    variants:
                      rare_sheep:
                        tier: rare
                        triggers:
                          on_shear:
                            actions:
                              - type: item_drop
                                material:
                """)))
                .isInstanceOf(ConfigValidationException.class)
                .hasMessageContaining("mobs.SHEEP.variants.rare_sheep.triggers.on_shear.actions[0].material")
                .hasMessageContaining("mobs.yml");
    }

    @Test
    void actionValuesExcludeType() {
        ConfigService service = ConfigService.fromResources();

        var action = service.snapshot()
                .mobProfiles()
                .get(EntityType.SHEEP)
                .variants()
                .get("rare_sheep")
                .triggers()
                .get("on_shear")
                .actions()
                .getFirst();

        assertThat(action.values()).doesNotContainKey("type");
    }

    private static YamlConfiguration yaml(String content) {
        return YamlConfiguration.loadConfiguration(new StringReader(content));
    }
}
