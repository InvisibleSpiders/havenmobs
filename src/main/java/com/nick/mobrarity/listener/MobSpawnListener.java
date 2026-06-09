package com.nick.mobrarity.listener;

import com.nick.mobrarity.config.ConfigSnapshot;
import com.nick.mobrarity.level.MobLevelService;
import com.nick.mobrarity.rarity.MobProfile;
import com.nick.mobrarity.rarity.MobVariant;
import com.nick.mobrarity.rarity.SpawnRarityService;
import com.nick.mobrarity.tag.MobRarityData;
import com.nick.mobrarity.tag.MobTagService;
import com.nick.mobrarity.stat.StatScalingService;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
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

    public MobSpawnListener(
            Map<EntityType, MobProfile> mobProfiles,
            SpawnRarityService spawnRarityService,
            MobLevelService mobLevelService,
            MobTagService mobTagService) {
        this(() -> new ConfigSnapshot(Map.of(), Map.copyOf(mobProfiles)), spawnRarityService, mobLevelService, mobTagService, null);
    }

    public MobSpawnListener(
            Supplier<ConfigSnapshot> configSupplier,
            SpawnRarityService spawnRarityService,
            MobLevelService mobLevelService,
            MobTagService mobTagService,
            StatScalingService statScalingService) {
        this.configSupplier = configSupplier;
        this.spawnRarityService = spawnRarityService;
        this.mobLevelService = mobLevelService;
        this.mobTagService = mobTagService;
        this.statScalingService = statScalingService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        processSpawn(
                event.getEntityType(),
                event.getSpawnReason(),
                event.getLocation().getY(),
                data -> {
                    mobTagService.tag(event.getEntity(), data);
                    applyScaling(event.getEntity(), data);
                });
    }

    void processSpawn(EntityType entityType, CreatureSpawnEvent.SpawnReason spawnReason, double y, TagCallback tagCallback) {
        ConfigSnapshot config = configSupplier.get();
        MobProfile profile = config.mobProfiles().get(entityType);
        if (profile == null) {
            return;
        }

        Optional<MobVariant> selectedVariant = spawnRarityService.selectVariant(profile, spawnReason);
        if (selectedVariant.isEmpty()) {
            return;
        }

        MobVariant variant = selectedVariant.get();
        int level = mobLevelService.calculateLevel(0, y);
        tagCallback.tag(new MobRarityData(variant.tierKey(), variant.key(), level));
    }

    private void applyScaling(org.bukkit.entity.LivingEntity entity, MobRarityData data) {
        if (statScalingService != null) {
            statScalingService.apply(configSupplier.get(), entity, data);
        }
    }

    @FunctionalInterface
    interface TagCallback {
        void tag(MobRarityData data);
    }
}
