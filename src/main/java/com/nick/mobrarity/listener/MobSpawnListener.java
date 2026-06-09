package com.nick.mobrarity.listener;

import com.nick.mobrarity.config.ConfigSnapshot;
import com.nick.mobrarity.level.MobLevelService;
import com.nick.mobrarity.rarity.MobProfile;
import com.nick.mobrarity.rarity.MobVariant;
import com.nick.mobrarity.rarity.SpawnRarityService;
import com.nick.mobrarity.tag.MobRarityData;
import com.nick.mobrarity.tag.MobTagService;
import com.nick.mobrarity.stat.StatScalingService;
import com.nick.mobrarity.visual.DisplayMode;
import com.nick.mobrarity.visual.VisualService;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

public final class MobSpawnListener implements Listener {
    private final Supplier<ConfigSnapshot> configSupplier;
    private final SpawnRarityService spawnRarityService;
    private final MobLevelService mobLevelService;
    private final MobTagService mobTagService;
    private final StatScalingService statScalingService;
    private final VisualService visualService;

    public MobSpawnListener(
            Map<EntityType, MobProfile> mobProfiles,
            SpawnRarityService spawnRarityService,
            MobLevelService mobLevelService,
            MobTagService mobTagService) {
        this(() -> new ConfigSnapshot(Map.of(), Map.copyOf(mobProfiles)), spawnRarityService, mobLevelService, mobTagService, null, null);
    }

    public MobSpawnListener(
            Supplier<ConfigSnapshot> configSupplier,
            SpawnRarityService spawnRarityService,
            MobLevelService mobLevelService,
            MobTagService mobTagService,
            StatScalingService statScalingService) {
        this(configSupplier, spawnRarityService, mobLevelService, mobTagService, statScalingService, null);
    }

    public MobSpawnListener(
            Supplier<ConfigSnapshot> configSupplier,
            SpawnRarityService spawnRarityService,
            MobLevelService mobLevelService,
            MobTagService mobTagService,
            StatScalingService statScalingService,
            VisualService visualService) {
        this.configSupplier = configSupplier;
        this.spawnRarityService = spawnRarityService;
        this.mobLevelService = mobLevelService;
        this.mobTagService = mobTagService;
        this.statScalingService = statScalingService;
        this.visualService = visualService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        ConfigSnapshot config = configSupplier.get();
        processSpawn(
                config,
                event.getEntityType(),
                event.getSpawnReason(),
                event.getLocation(),
                data -> {
                    mobTagService.tag(event.getEntity(), data);
                    applyScaling(config, event.getEntity(), data);
                    applyVisuals(config, event.getEntity(), data);
                });
    }

    void processSpawn(
            EntityType entityType,
            CreatureSpawnEvent.SpawnReason spawnReason,
            Location location,
            TagCallback tagCallback) {
        ConfigSnapshot config = configSupplier.get();
        processSpawn(config, entityType, spawnReason, location, tagCallback);
    }

    private void processSpawn(
            ConfigSnapshot config,
            EntityType entityType,
            CreatureSpawnEvent.SpawnReason spawnReason,
            Location location,
            TagCallback tagCallback) {
        MobProfile profile = config.mobProfiles().get(entityType);
        if (profile == null) {
            return;
        }

        Optional<MobVariant> selectedVariant = spawnRarityService.selectVariant(profile, spawnReason);
        if (selectedVariant.isEmpty()) {
            return;
        }

        MobVariant variant = selectedVariant.get();
        int level = mobLevelService.calculateLevel(horizontalDistanceFromSpawn(location), location.getY());
        tagCallback.tag(new MobRarityData(variant.tierKey(), variant.key(), level));
    }

    private void applyScaling(ConfigSnapshot config, org.bukkit.entity.LivingEntity entity, MobRarityData data) {
        if (statScalingService != null) {
            statScalingService.apply(config, entity, data);
        }
    }

    private void applyVisuals(ConfigSnapshot config, org.bukkit.entity.LivingEntity entity, MobRarityData data) {
        if (visualService != null) {
            visualService.applyNametag(config, entity, data, DisplayMode.TARGETED);
        }
    }

    private static double horizontalDistanceFromSpawn(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return 0.0;
        }
        Location spawn = world.getSpawnLocation();
        double x = location.getX() - spawn.getX();
        double z = location.getZ() - spawn.getZ();
        return Math.sqrt((x * x) + (z * z));
    }

    @FunctionalInterface
    interface TagCallback {
        void tag(MobRarityData data);
    }
}
