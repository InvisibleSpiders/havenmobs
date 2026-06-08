package com.nick.mobrarity.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.junit.jupiter.api.Test;

final class MobRarityAdminServiceTest {
    @Test
    void inspectReportsTaggedTarget() {
        LivingEntity target = livingEntity(EntityType.SHEEP);
        MobTagService tagService = mock(MobTagService.class);
        when(tagService.read(target)).thenReturn(Optional.of(new MobRarityData("rare", "rare_sheep", 7)));
        MobRarityAdminService service = service(tagService, Optional.of(target), config());

        AdminCommandResult result = service.inspect(mock(Player.class));

        assertThat(result.success()).isTrue();
        assertThat(result.message()).isEqualTo("Target: SHEEP rare/rare_sheep level 7.");
    }

    @Test
    void setTagsTargetWhenVariantBelongsToTargetEntity() {
        LivingEntity target = livingEntity(EntityType.SHEEP);
        MobTagService tagService = mock(MobTagService.class);
        MobRarityAdminService service = service(tagService, Optional.of(target), config());

        AdminCommandResult result = service.set(mock(Player.class), "rare", "rare_sheep", 5);

        assertThat(result.success()).isTrue();
        assertThat(result.message()).isEqualTo("Set SHEEP to rare/rare_sheep level 5.");
        verify(tagService).tag(target, new MobRarityData("rare", "rare_sheep", 5));
    }

    @Test
    void setRejectsVariantThatDoesNotBelongToTargetEntity() {
        LivingEntity target = livingEntity(EntityType.ZOMBIE);
        MobRarityAdminService service = service(mock(MobTagService.class), Optional.of(target), config());

        AdminCommandResult result = service.set(mock(Player.class), "rare", "rare_sheep", 5);

        assertThat(result.success()).isFalse();
        assertThat(result.message()).isEqualTo("Variant 'rare_sheep' is not configured for ZOMBIE.");
    }

    @Test
    void clearRemovesTargetTag() {
        LivingEntity target = livingEntity(EntityType.SHEEP);
        MobTagService tagService = mock(MobTagService.class);
        MobRarityAdminService service = service(tagService, Optional.of(target), config());

        AdminCommandResult result = service.clear(mock(Player.class));

        assertThat(result.success()).isTrue();
        assertThat(result.message()).isEqualTo("Cleared MobRarity data from SHEEP.");
        verify(tagService).clear(target);
    }

    @Test
    void spawnCreatesLivingEntityAtTargetPlayerAndTagsIt() {
        Player targetPlayer = mock(Player.class);
        Location location = mock(Location.class);
        World world = mock(World.class);
        LivingEntity spawned = livingEntity(EntityType.SHEEP);
        when(targetPlayer.getName()).thenReturn("Alex");
        when(targetPlayer.getLocation()).thenReturn(location);
        when(location.getWorld()).thenReturn(world);
        when(world.spawnEntity(location, EntityType.SHEEP)).thenReturn(spawned);
        MobTagService tagService = mock(MobTagService.class);
        MobRarityAdminService service = service(tagService, Optional.empty(), config());

        AdminCommandResult result = service.spawn(
                mock(CommandSender.class), targetPlayer, EntityType.SHEEP, "rare", "rare_sheep", 3);

        assertThat(result.success()).isTrue();
        assertThat(result.message()).isEqualTo("Spawned SHEEP rare/rare_sheep level 3 on Alex.");
        verify(tagService).tag(spawned, new MobRarityData("rare", "rare_sheep", 3));
    }

    @Test
    void spawnRejectsNonLivingEntityTypes() {
        Player targetPlayer = mock(Player.class);
        MobRarityAdminService service = service(mock(MobTagService.class), Optional.empty(), config());

        AdminCommandResult result = service.spawn(
                mock(CommandSender.class), targetPlayer, EntityType.ITEM, "rare", "rare_sheep", 3);

        assertThat(result.success()).isFalse();
        assertThat(result.message()).isEqualTo("ITEM is not a living mob type.");
    }

    @Test
    void completionsIncludeConfiguredTiersAndVariants() {
        MobRarityAdminService service = service(mock(MobTagService.class), Optional.empty(), config());

        assertThat(service.completions())
                .containsEntry("tiers", List.of("rare"))
                .containsEntry("variants", List.of("rare_sheep"));
    }

    @Test
    void listReportsConfiguredTiersVariantsAndMobs() {
        MobRarityAdminService service = service(mock(MobTagService.class), Optional.empty(), config());

        assertThat(service.list("tiers").message()).isEqualTo("Tiers: rare");
        assertThat(service.list("variants").message()).isEqualTo("Variants: rare_sheep");
        assertThat(service.list("mobs").message()).isEqualTo("Mobs: SHEEP");
    }

    @Test
    void listRejectsUnknownCategoriesWithUsage() {
        MobRarityAdminService service = service(mock(MobTagService.class), Optional.empty(), config());

        AdminCommandResult result = service.list("loot");

        assertThat(result.success()).isFalse();
        assertThat(result.message()).isEqualTo("Unknown list category 'loot'. Use tiers, variants, or mobs.");
    }

    private static MobRarityAdminService service(
            MobTagService tagService,
            Optional<LivingEntity> target,
            ConfigSnapshot snapshot) {
        return new MobRarityAdminService(() -> snapshot, tagService, player -> target);
    }

    private static LivingEntity livingEntity(EntityType entityType) {
        LivingEntity entity = mock(LivingEntity.class);
        when(entity.getType()).thenReturn(entityType);
        return entity;
    }

    private static ConfigSnapshot config() {
        MobVariant variant = new MobVariant("rare_sheep", "rare", 1.0, "<aqua>Rare Sheep</aqua>", Map.of());
        MobProfile sheep = new MobProfile(Map.of(SpawnReason.NATURAL, true), Map.of(variant.key(), variant));
        RarityTier tier = new RarityTier("rare", 1.0, "<aqua>Rare</aqua>", Map.of());
        return new ConfigSnapshot(Map.of(tier.key(), tier), Map.of(EntityType.SHEEP, sheep));
    }
}
