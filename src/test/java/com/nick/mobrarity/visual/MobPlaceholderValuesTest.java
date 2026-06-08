package com.nick.mobrarity.visual;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nick.mobrarity.config.ConfigSnapshot;
import com.nick.mobrarity.rarity.MobProfile;
import com.nick.mobrarity.rarity.MobVariant;
import com.nick.mobrarity.rarity.RarityTier;
import com.nick.mobrarity.tag.MobRarityData;
import java.util.Map;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.junit.jupiter.api.Test;

final class MobPlaceholderValuesTest {
    @Test
    void createsBuiltInPlaceholderValuesForTaggedMob() {
        LivingEntity entity = mock(LivingEntity.class);
        when(entity.getType()).thenReturn(EntityType.SHEEP);

        Map<String, String> values = MobPlaceholderValues.from(
                snapshot(),
                entity,
                new MobRarityData("rare", "toxic_sheep", 7));

        assertThat(values)
                .containsEntry("mobrarity_tier", "Rare")
                .containsEntry("mobrarity_tier_key", "rare")
                .containsEntry("mobrarity_variant", "Toxic Sheep")
                .containsEntry("mobrarity_variant_key", "toxic_sheep")
                .containsEntry("mobrarity_level", "7")
                .containsEntry("mobrarity_entity", "Sheep");
    }

    @Test
    void fallsBackToHumanizedKeysWhenConfigEntriesAreMissing() {
        LivingEntity entity = mock(LivingEntity.class);
        when(entity.getType()).thenReturn(EntityType.ZOMBIE_VILLAGER);

        Map<String, String> values = MobPlaceholderValues.from(
                new ConfigSnapshot(Map.of(), Map.of()),
                entity,
                new MobRarityData("legendary_boss", "angry_zombie", 12));

        assertThat(values)
                .containsEntry("mobrarity_tier", "Legendary Boss")
                .containsEntry("mobrarity_variant", "Angry Zombie")
                .containsEntry("mobrarity_entity", "Zombie Villager");
    }

    private static ConfigSnapshot snapshot() {
        RarityTier tier = new RarityTier("rare", 1.0, "<aqua>Rare</aqua>", Map.of());
        MobVariant variant = new MobVariant("toxic_sheep", "rare", 1.0, null, Map.of());
        MobProfile profile = new MobProfile(Map.of(SpawnReason.NATURAL, true), Map.of("toxic_sheep", variant));
        return new ConfigSnapshot(Map.of("rare", tier), Map.of(EntityType.SHEEP, profile));
    }
}
