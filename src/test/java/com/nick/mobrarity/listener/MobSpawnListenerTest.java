package com.nick.mobrarity.listener;

import static org.assertj.core.api.Assertions.assertThat;

import com.nick.mobrarity.level.LevelSettings;
import com.nick.mobrarity.level.MobLevelService;
import com.nick.mobrarity.rarity.MobProfile;
import com.nick.mobrarity.rarity.MobVariant;
import com.nick.mobrarity.rarity.SpawnRarityService;
import com.nick.mobrarity.tag.MobRarityData;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.junit.jupiter.api.Test;

final class MobSpawnListenerTest {
    @Test
    void creatureSpawnHandlerIgnoresCancelledEvents() throws Exception {
        Method method = MobSpawnListener.class.getDeclaredMethod("onCreatureSpawn", CreatureSpawnEvent.class);
        EventHandler handler = method.getAnnotation(EventHandler.class);

        assertThat(handler.ignoreCancelled()).isTrue();
    }

    @Test
    void skipsSpawnWhenProfileAbsent() {
        List<MobRarityData> tagged = new ArrayList<>();
        MobSpawnListener listener = listener(Map.of(), Optional.empty());

        listener.processSpawn(EntityType.SHEEP, SpawnReason.NATURAL, 44, tagged::add);

        assertThat(tagged).isEmpty();
    }

    @Test
    void skipsSpawnWhenReasonDenied() {
        MobVariant variant = variant();
        MobProfile profile = new MobProfile(Map.of(SpawnReason.SPAWNER, false), Map.of(variant.key(), variant));
        List<MobRarityData> tagged = new ArrayList<>();
        MobSpawnListener listener = listener(Map.of(EntityType.SHEEP, profile), Optional.of(variant));

        listener.processSpawn(EntityType.SHEEP, SpawnReason.SPAWNER, 44, tagged::add);

        assertThat(tagged).isEmpty();
    }

    @Test
    void skipsSpawnWhenNoVariantSelected() {
        MobVariant variant = variant();
        MobProfile profile = new MobProfile(Map.of(SpawnReason.NATURAL, true), Map.of(variant.key(), variant));
        List<MobRarityData> tagged = new ArrayList<>();
        MobSpawnListener listener = listener(Map.of(EntityType.SHEEP, profile), Optional.empty());

        listener.processSpawn(EntityType.SHEEP, SpawnReason.NATURAL, 44, tagged::add);

        assertThat(tagged).isEmpty();
    }

    @Test
    void tagsSelectedVariantWithLevelCalculatedFromY() {
        MobVariant variant = variant();
        MobProfile profile = new MobProfile(Map.of(SpawnReason.NATURAL, true), Map.of(variant.key(), variant));
        List<MobRarityData> tagged = new ArrayList<>();
        MobSpawnListener listener = listener(Map.of(EntityType.SHEEP, profile), Optional.of(variant));

        listener.processSpawn(EntityType.SHEEP, SpawnReason.NATURAL, 44, tagged::add);

        assertThat(tagged).containsExactly(new MobRarityData("rare", "rare_sheep", 4));
    }

    private static MobSpawnListener listener(Map<EntityType, MobProfile> profiles, Optional<MobVariant> selected) {
        SpawnRarityService spawnRarityService = new SpawnRarityService(entries -> selected);
        MobLevelService mobLevelService = new MobLevelService(new LevelSettings(true, 250, 1, 100, true, 64, 0.15, 25));
        return new MobSpawnListener(profiles, spawnRarityService, mobLevelService, null);
    }

    private static MobVariant variant() {
        return new MobVariant("rare_sheep", "rare", 1, "<aqua>Rare Sheep</aqua>", Map.of());
    }
}
