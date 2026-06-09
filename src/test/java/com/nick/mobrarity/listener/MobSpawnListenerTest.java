package com.nick.mobrarity.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import org.bukkit.Location;
import org.bukkit.World;
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

        listener.processSpawn(EntityType.SHEEP, SpawnReason.NATURAL, location(0, 44, 0), tagged::add);

        assertThat(tagged).isEmpty();
    }

    @Test
    void skipsSpawnWhenReasonDenied() {
        MobVariant variant = variant();
        MobProfile profile = new MobProfile(Map.of(SpawnReason.SPAWNER, false), Map.of(variant.key(), variant));
        List<MobRarityData> tagged = new ArrayList<>();
        MobSpawnListener listener = listener(Map.of(EntityType.SHEEP, profile), Optional.of(variant));

        listener.processSpawn(EntityType.SHEEP, SpawnReason.SPAWNER, location(0, 44, 0), tagged::add);

        assertThat(tagged).isEmpty();
    }

    @Test
    void skipsSpawnWhenNoVariantSelected() {
        MobVariant variant = variant();
        MobProfile profile = new MobProfile(Map.of(SpawnReason.NATURAL, true), Map.of(variant.key(), variant));
        List<MobRarityData> tagged = new ArrayList<>();
        MobSpawnListener listener = listener(Map.of(EntityType.SHEEP, profile), Optional.empty());

        listener.processSpawn(EntityType.SHEEP, SpawnReason.NATURAL, location(0, 44, 0), tagged::add);

        assertThat(tagged).isEmpty();
    }

    @Test
    void tagsSelectedVariantWithLevelCalculatedFromYAndHorizontalDistance() {
        MobVariant variant = variant();
        MobProfile profile = new MobProfile(Map.of(SpawnReason.NATURAL, true), Map.of(variant.key(), variant));
        List<MobRarityData> tagged = new ArrayList<>();
        MobSpawnListener listener = listener(Map.of(EntityType.SHEEP, profile), Optional.of(variant));

        listener.processSpawn(EntityType.SHEEP, SpawnReason.NATURAL, location(500, 44, 0), tagged::add);

        assertThat(tagged).containsExactly(new MobRarityData("rare", "rare_sheep", 6));
    }

    private static MobSpawnListener listener(Map<EntityType, MobProfile> profiles, Optional<MobVariant> selected) {
        SpawnRarityService spawnRarityService = new SpawnRarityService(entries -> selected);
        MobLevelService mobLevelService = new MobLevelService(new LevelSettings(true, 250, 1, 100, true, 64, 0.15, 25));
        return new MobSpawnListener(profiles, spawnRarityService, mobLevelService, null);
    }

    private static MobVariant variant() {
        return new MobVariant("rare_sheep", "rare", 1, "<aqua>Rare Sheep</aqua>", Map.of());
    }

    private static Location location(double x, double y, double z) {
        World world = mock(World.class);
        Location spawn = mock(Location.class);
        Location location = mock(Location.class);
        when(location.getWorld()).thenReturn(world);
        when(location.getX()).thenReturn(x);
        when(location.getY()).thenReturn(y);
        when(location.getZ()).thenReturn(z);
        when(world.getSpawnLocation()).thenReturn(spawn);
        when(spawn.getX()).thenReturn(0.0);
        when(spawn.getZ()).thenReturn(0.0);
        return location;
    }
}
