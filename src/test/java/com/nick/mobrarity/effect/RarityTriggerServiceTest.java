package com.nick.mobrarity.effect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nick.mobrarity.config.ConfigSnapshot;
import com.nick.mobrarity.rarity.MobProfile;
import com.nick.mobrarity.rarity.MobVariant;
import com.nick.mobrarity.rarity.RarityTier;
import com.nick.mobrarity.tag.MobRarityData;
import com.nick.mobrarity.tag.MobTagService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.junit.jupiter.api.Test;

final class RarityTriggerServiceTest {
    @Test
    void resolvesVariantTriggerForTaggedMob() {
        LivingEntity entity = mockEntity(EntityType.SHEEP);
        MobTagService tagService = mock(MobTagService.class);
        when(tagService.read(entity)).thenReturn(Optional.of(new MobRarityData("rare", "rare_sheep", 3)));
        TriggerDefinition trigger = trigger("on_shear", "item_drop");
        ConfigSnapshot snapshot = snapshot(
                new MobVariant("rare_sheep", "rare", 1.0, null, Map.of("on_shear", trigger)),
                new RarityTier("rare", 1.0, "Rare", Map.of()));
        RarityTriggerService service = new RarityTriggerService(() -> snapshot, tagService);

        assertThat(service.trigger(entity, "on_shear")).contains(trigger);
    }

    @Test
    void fallsBackToTierTriggerWhenVariantDoesNotDefineTrigger() {
        LivingEntity entity = mockEntity(EntityType.SHEEP);
        MobTagService tagService = mock(MobTagService.class);
        when(tagService.read(entity)).thenReturn(Optional.of(new MobRarityData("rare", "rare_sheep", 3)));
        TriggerDefinition trigger = trigger("on_death", "currency_drop");
        ConfigSnapshot snapshot = snapshot(
                new MobVariant("rare_sheep", "rare", 1.0, null, Map.of()),
                new RarityTier("rare", 1.0, "Rare", Map.of("on_death", trigger)));
        RarityTriggerService service = new RarityTriggerService(() -> snapshot, tagService);

        assertThat(service.trigger(entity, "on_death")).contains(trigger);
    }

    @Test
    void returnsEmptyWhenMobIsNotTagged() {
        LivingEntity entity = mockEntity(EntityType.SHEEP);
        MobTagService tagService = mock(MobTagService.class);
        when(tagService.read(entity)).thenReturn(Optional.empty());
        RarityTriggerService service = new RarityTriggerService(() -> snapshot(
                new MobVariant("rare_sheep", "rare", 1.0, null, Map.of()),
                new RarityTier("rare", 1.0, "Rare", Map.of())),
                tagService);

        assertThat(service.trigger(entity, "on_shear")).isEmpty();
    }

    private static LivingEntity mockEntity(EntityType entityType) {
        LivingEntity entity = mock(LivingEntity.class);
        when(entity.getType()).thenReturn(entityType);
        return entity;
    }

    private static TriggerDefinition trigger(String key, String actionType) {
        return new TriggerDefinition(key, 1.0, 0, 0,
                List.of(new ActionDefinition(actionType, Map.of())));
    }

    private static ConfigSnapshot snapshot(MobVariant variant, RarityTier tier) {
        return new ConfigSnapshot(
                Map.of(tier.key(), tier),
                Map.of(EntityType.SHEEP, new MobProfile(
                        Map.of(SpawnReason.NATURAL, true),
                        Map.of(variant.key(), variant))));
    }
}
