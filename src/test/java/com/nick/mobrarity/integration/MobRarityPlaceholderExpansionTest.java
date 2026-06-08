package com.nick.mobrarity.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nick.mobrarity.command.TargetResolver;
import com.nick.mobrarity.config.ConfigSnapshot;
import com.nick.mobrarity.rarity.MobProfile;
import com.nick.mobrarity.rarity.MobVariant;
import com.nick.mobrarity.rarity.RarityTier;
import com.nick.mobrarity.tag.MobRarityData;
import com.nick.mobrarity.tag.MobTagService;
import java.util.Map;
import java.util.Optional;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.junit.jupiter.api.Test;

final class MobRarityPlaceholderExpansionTest {
    @Test
    void resolvesPlaceholderFromPlayersTargetedMob() {
        Player player = mock(Player.class);
        LivingEntity target = mock(LivingEntity.class);
        when(target.getType()).thenReturn(EntityType.SHEEP);
        MobTagService tagService = mock(MobTagService.class);
        when(tagService.read(target)).thenReturn(Optional.of(new MobRarityData("rare", "toxic_sheep", 9)));
        TargetResolver targetResolver = ignored -> Optional.of(target);
        MobRarityPlaceholderExpansion expansion = new MobRarityPlaceholderExpansion(
                () -> snapshot(),
                tagService,
                targetResolver,
                "1.0.0");

        assertThat(expansion.onPlaceholderRequest(player, "tier")).isEqualTo("Rare");
        assertThat(expansion.onPlaceholderRequest(player, "variant_key")).isEqualTo("toxic_sheep");
        assertThat(expansion.onPlaceholderRequest(player, "level")).isEqualTo("9");
    }

    @Test
    void returnsEmptyForUnknownPlaceholderOrMissingTarget() {
        Player player = mock(Player.class);
        MobRarityPlaceholderExpansion expansion = new MobRarityPlaceholderExpansion(
                () -> snapshot(),
                mock(MobTagService.class),
                ignored -> Optional.empty(),
                "1.0.0");

        assertThat(expansion.onPlaceholderRequest(player, "tier")).isEmpty();
        assertThat(expansion.onPlaceholderRequest(player, "missing")).isEmpty();
    }

    @Test
    void exposesExpansionMetadata() {
        MobRarityPlaceholderExpansion expansion = new MobRarityPlaceholderExpansion(
                () -> snapshot(),
                mock(MobTagService.class),
                ignored -> Optional.empty(),
                "1.2.3");

        assertThat(expansion.getIdentifier()).isEqualTo("mobrarity");
        assertThat(expansion.getAuthor()).isEqualTo("Nick");
        assertThat(expansion.getVersion()).isEqualTo("1.2.3");
        assertThat(expansion.persist()).isTrue();
    }

    private static ConfigSnapshot snapshot() {
        RarityTier tier = new RarityTier("rare", 1.0, "<aqua>Rare</aqua>", Map.of());
        MobVariant variant = new MobVariant("toxic_sheep", "rare", 1.0, null, Map.of());
        MobProfile profile = new MobProfile(Map.of(SpawnReason.NATURAL, true), Map.of("toxic_sheep", variant));
        return new ConfigSnapshot(Map.of("rare", tier), Map.of(EntityType.SHEEP, profile));
    }
}
