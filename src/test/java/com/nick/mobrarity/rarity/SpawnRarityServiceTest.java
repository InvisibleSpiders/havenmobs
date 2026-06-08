package com.nick.mobrarity.rarity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Optional;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.junit.jupiter.api.Test;

final class SpawnRarityServiceTest {
    @Test
    void selectsVariantWhenSpawnReasonAllowed() {
        MobVariant variant = new MobVariant("rare_sheep", "rare", 3, "<aqua>Rare Sheep</aqua>", Map.of());
        MobProfile profile = new MobProfile(Map.of(SpawnReason.NATURAL, true), Map.of("rare_sheep", variant));
        SpawnRarityService service = new SpawnRarityService(entries -> Optional.of(variant));

        Optional<MobVariant> selected = service.selectVariant(profile, SpawnReason.NATURAL);

        assertThat(selected).contains(variant);
    }

    @Test
    void skipsVariantWhenSpawnReasonDenied() {
        MobVariant variant = new MobVariant("rare_sheep", "rare", 3, "<aqua>Rare Sheep</aqua>", Map.of());
        MobProfile profile = new MobProfile(Map.of(SpawnReason.SPAWNER, false), Map.of("rare_sheep", variant));
        SpawnRarityService service = new SpawnRarityService(entries -> Optional.of(variant));

        assertThat(service.selectVariant(profile, SpawnReason.SPAWNER)).isEmpty();
    }
}
