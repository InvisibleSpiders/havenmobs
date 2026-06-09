package com.nick.mobrarity.stat;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;

public final class BukkitStatAttributeAccessor implements StatAttributeAccessor {
    private static final Map<String, String> ATTRIBUTE_KEYS = Map.of(
            "max-health", "max_health",
            "attack-damage", "attack_damage",
            "movement-speed", "movement_speed",
            "armor", "armor",
            "armor-toughness", "armor_toughness",
            "knockback-resistance", "knockback_resistance",
            "follow-range", "follow_range");

    @Override
    public OptionalDouble baseValue(LivingEntity entity, String statKey) {
        AttributeInstance instance = attribute(entity, statKey).orElse(null);
        return instance == null ? OptionalDouble.empty() : OptionalDouble.of(instance.getBaseValue());
    }

    @Override
    public void setBaseValue(LivingEntity entity, String statKey, double value) {
        attribute(entity, statKey).ifPresent(instance -> instance.setBaseValue(value));
    }

    @Override
    public void healToMax(LivingEntity entity) {
        baseValue(entity, "max-health").ifPresent(maxHealth -> entity.setHealth(Math.max(0.0, maxHealth)));
    }

    private static Optional<AttributeInstance> attribute(LivingEntity entity, String statKey) {
        return attribute(statKey).map(entity::getAttribute).filter(instance -> instance != null);
    }

    private static Optional<Attribute> attribute(String statKey) {
        String key = ATTRIBUTE_KEYS.get(statKey);
        if (key == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(Registry.ATTRIBUTE.get(NamespacedKey.minecraft(key)));
    }
}
