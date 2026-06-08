package com.nick.mobrarity.tag;

import java.util.Optional;
import java.util.Objects;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class MobTagService {
    private final NamespacedKey rarityDataKey;

    public MobTagService(Plugin plugin) {
        this.rarityDataKey = new NamespacedKey(plugin, "rarity_data");
    }

    MobTagService(NamespacedKey rarityDataKey) {
        this.rarityDataKey = Objects.requireNonNull(rarityDataKey, "rarityDataKey");
    }

    public void tag(LivingEntity entity, MobRarityData data) {
        entity.getPersistentDataContainer().set(rarityDataKey, PersistentDataType.STRING, data.serialize());
    }

    public Optional<MobRarityData> read(LivingEntity entity) {
        String value = entity.getPersistentDataContainer().get(rarityDataKey, PersistentDataType.STRING);
        return MobRarityData.parse(value);
    }

    public void clear(LivingEntity entity) {
        entity.getPersistentDataContainer().remove(rarityDataKey);
    }
}
