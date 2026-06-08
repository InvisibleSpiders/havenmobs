package com.nick.mobrarity.listener;

import com.nick.mobrarity.level.MobLevelService;
import com.nick.mobrarity.rarity.MobProfile;
import com.nick.mobrarity.rarity.MobVariant;
import com.nick.mobrarity.rarity.SpawnRarityService;
import com.nick.mobrarity.tag.MobRarityData;
import com.nick.mobrarity.tag.MobTagService;
import java.util.Map;
import java.util.Optional;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

public final class MobSpawnListener implements Listener {
    private final Map<EntityType, MobProfile> mobProfiles;
    private final SpawnRarityService spawnRarityService;
    private final MobLevelService mobLevelService;
    private final MobTagService mobTagService;

    public MobSpawnListener(
            Map<EntityType, MobProfile> mobProfiles,
            SpawnRarityService spawnRarityService,
            MobLevelService mobLevelService,
            MobTagService mobTagService) {
        this.mobProfiles = Map.copyOf(mobProfiles);
        this.spawnRarityService = spawnRarityService;
        this.mobLevelService = mobLevelService;
        this.mobTagService = mobTagService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        processSpawn(
                event.getEntityType(),
                event.getSpawnReason(),
                event.getLocation().getY(),
                data -> mobTagService.tag(event.getEntity(), data));
    }

    void processSpawn(EntityType entityType, CreatureSpawnEvent.SpawnReason spawnReason, double y, TagCallback tagCallback) {
        MobProfile profile = mobProfiles.get(entityType);
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

    @FunctionalInterface
    interface TagCallback {
        void tag(MobRarityData data);
    }
}
