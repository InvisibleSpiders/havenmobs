package com.nick.mobrarity.visual;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nick.mobrarity.config.ConfigSnapshot;
import com.nick.mobrarity.integration.TextPlaceholderService;
import com.nick.mobrarity.rarity.MobProfile;
import com.nick.mobrarity.rarity.MobVariant;
import com.nick.mobrarity.rarity.RarityTier;
import com.nick.mobrarity.tag.MobRarityData;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

final class VisualServiceTest {
    @Test
    void appliesVariantNametagAndKeepsTargetedNamesHiddenUntilLookedAt() {
        LivingEntity entity = mock(LivingEntity.class);
        when(entity.getType()).thenReturn(EntityType.SHEEP);
        VisualService service = new VisualService(new MobTextFormatter(new TextPlaceholderService(false)));

        service.applyNametag(snapshot("<green>[Lv %mobrarity_level%] Toxic Sheep</green>", null),
                entity,
                new MobRarityData("rare", "toxic_sheep", 7),
                DisplayMode.TARGETED);

        ArgumentCaptor<Component> name = ArgumentCaptor.forClass(Component.class);
        verify(entity).customName(name.capture());
        assertThat(PlainTextComponentSerializer.plainText().serialize(name.getValue()))
                .isEqualTo("[Lv 7] Toxic Sheep");
        verify(entity).setCustomNameVisible(false);
    }

    @Test
    void fallsBackToTierNametagWhenVariantHasNoTemplate() {
        LivingEntity entity = mock(LivingEntity.class);
        when(entity.getType()).thenReturn(EntityType.SHEEP);
        VisualService service = new VisualService(new MobTextFormatter(new TextPlaceholderService(false)));

        service.applyNametag(snapshot(null, "<aqua>[Lv %mobrarity_level%] %mobrarity_variant%</aqua>"),
                entity,
                new MobRarityData("rare", "toxic_sheep", 3),
                DisplayMode.ALWAYS);

        ArgumentCaptor<Component> name = ArgumentCaptor.forClass(Component.class);
        verify(entity).customName(name.capture());
        assertThat(PlainTextComponentSerializer.plainText().serialize(name.getValue()))
                .isEqualTo("[Lv 3] Toxic Sheep");
        verify(entity).setCustomNameVisible(true);
    }

    @Test
    void disabledModeClearsNametag() {
        LivingEntity entity = mock(LivingEntity.class);
        VisualService service = new VisualService(new MobTextFormatter(new TextPlaceholderService(false)));

        service.applyNametag(new ConfigSnapshot(Map.of(), Map.of()),
                entity,
                new MobRarityData("rare", "toxic_sheep", 1),
                DisplayMode.DISABLED);

        verify(entity).customName(null);
        verify(entity).setCustomNameVisible(false);
    }

    @Test
    void clearNametagRemovesCustomNameAndVisibility() {
        LivingEntity entity = mock(LivingEntity.class);
        VisualService service = new VisualService(new MobTextFormatter(new TextPlaceholderService(false)));

        service.clearNametag(entity);

        verify(entity).customName(null);
        verify(entity).setCustomNameVisible(false);
    }

    private static ConfigSnapshot snapshot(String variantNametag, String tierNametag) {
        RarityTier tier = new RarityTier("rare", 1.0, "<aqua>Rare</aqua>", tierNametag, Map.of(), Map.of());
        MobVariant variant = new MobVariant("toxic_sheep", "rare", 1.0, variantNametag, Map.of());
        MobProfile profile = new MobProfile(Map.of(SpawnReason.NATURAL, true), Map.of("toxic_sheep", variant));
        return new ConfigSnapshot(Map.of("rare", tier), Map.of(EntityType.SHEEP, profile));
    }
}
