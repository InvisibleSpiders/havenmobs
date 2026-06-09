package com.nick.mobrarity.stat;

import com.nick.mobrarity.config.ConfigSnapshot;
import com.nick.mobrarity.rarity.MobProfile;
import com.nick.mobrarity.rarity.MobVariant;
import com.nick.mobrarity.rarity.RarityTier;
import com.nick.mobrarity.tag.MobRarityData;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import org.bukkit.entity.LivingEntity;

public final class StatScalingService {
    private static final String MAX_HEALTH = "max-health";

    private final StatAttributeAccessor attributeAccessor;

    public StatScalingService(StatAttributeAccessor attributeAccessor) {
        this.attributeAccessor = Objects.requireNonNull(attributeAccessor, "attributeAccessor");
    }

    public void apply(ConfigSnapshot snapshot, LivingEntity entity, MobRarityData data) {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(data, "data");

        boolean changedMaxHealth = applyModifiers(entity, data.level(), tierModifiers(snapshot, data));
        changedMaxHealth = applyModifiers(entity, data.level(), variantModifiers(snapshot, entity, data)) || changedMaxHealth;
        if (changedMaxHealth) {
            attributeAccessor.healToMax(entity);
        }
    }

    private boolean applyModifiers(LivingEntity entity, int level, Map<String, StatModifier> modifiers) {
        boolean changedMaxHealth = false;
        for (Map.Entry<String, StatModifier> entry : modifiers.entrySet()) {
            OptionalDouble baseValue = attributeAccessor.baseValue(entity, entry.getKey());
            if (baseValue.isEmpty()) {
                continue;
            }
            double scaledValue = entry.getValue().apply(baseValue.getAsDouble(), level);
            if (!Double.isFinite(scaledValue) || scaledValue < 0) {
                continue;
            }
            attributeAccessor.setBaseValue(entity, entry.getKey(), scaledValue);
            changedMaxHealth = changedMaxHealth || MAX_HEALTH.equals(entry.getKey());
        }
        return changedMaxHealth;
    }

    private static Map<String, StatModifier> tierModifiers(ConfigSnapshot snapshot, MobRarityData data) {
        RarityTier tier = snapshot.tiers().get(data.tierKey());
        return tier == null ? Map.of() : tier.stats();
    }

    private static Map<String, StatModifier> variantModifiers(
            ConfigSnapshot snapshot,
            LivingEntity entity,
            MobRarityData data) {
        MobProfile profile = snapshot.mobProfiles().get(entity.getType());
        if (profile != null) {
            MobVariant variant = profile.variants().get(data.variantKey());
            if (variant != null) {
                return variant.stats();
            }
        }
        return Map.of();
    }
}
