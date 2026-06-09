package com.nick.mobrarity.stat;

import com.nick.mobrarity.config.ConfigSnapshot;
import com.nick.mobrarity.rarity.MobProfile;
import com.nick.mobrarity.rarity.MobVariant;
import com.nick.mobrarity.rarity.RarityTier;
import com.nick.mobrarity.tag.MobRarityData;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.WeakHashMap;
import org.bukkit.entity.LivingEntity;

public final class StatScalingService {
    private static final String MAX_HEALTH = "max-health";

    private final StatAttributeAccessor attributeAccessor;
    private final StatBaselineStore baselineStore;

    public StatScalingService(StatAttributeAccessor attributeAccessor) {
        this(attributeAccessor, new MemoryStatBaselineStore());
    }

    public StatScalingService(StatAttributeAccessor attributeAccessor, StatBaselineStore baselineStore) {
        this.attributeAccessor = Objects.requireNonNull(attributeAccessor, "attributeAccessor");
        this.baselineStore = Objects.requireNonNull(baselineStore, "baselineStore");
    }

    public void apply(ConfigSnapshot snapshot, LivingEntity entity, MobRarityData data) {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(data, "data");

        Map<String, Double> workingValues = new HashMap<>();
        boolean changedMaxHealth = applyModifiers(entity, data.level(), tierModifiers(snapshot, data), workingValues);
        changedMaxHealth = applyModifiers(entity, data.level(), variantModifiers(snapshot, entity, data), workingValues)
                || changedMaxHealth;
        if (changedMaxHealth) {
            attributeAccessor.healToMax(entity);
        }
    }

    public void clear(LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");

        boolean changedMaxHealth = false;
        for (Map.Entry<String, Double> entry : baselineStore.baselines(entity).entrySet()) {
            if (!Double.isFinite(entry.getValue()) || entry.getValue() < 0) {
                continue;
            }
            attributeAccessor.setBaseValue(entity, entry.getKey(), entry.getValue());
            changedMaxHealth = changedMaxHealth || MAX_HEALTH.equals(entry.getKey());
        }
        baselineStore.clear(entity);
        if (changedMaxHealth) {
            attributeAccessor.healToMax(entity);
        }
    }

    private boolean applyModifiers(
            LivingEntity entity,
            int level,
            Map<String, StatModifier> modifiers,
            Map<String, Double> workingValues) {
        boolean changedMaxHealth = false;
        for (Map.Entry<String, StatModifier> entry : modifiers.entrySet()) {
            String statKey = entry.getKey();
            OptionalDouble baseValue = workingValue(entity, statKey, workingValues);
            if (baseValue.isEmpty()) {
                continue;
            }
            double scaledValue = entry.getValue().apply(baseValue.getAsDouble(), level);
            if (!Double.isFinite(scaledValue) || scaledValue < 0) {
                continue;
            }
            workingValues.put(statKey, scaledValue);
            attributeAccessor.setBaseValue(entity, statKey, scaledValue);
            changedMaxHealth = changedMaxHealth || MAX_HEALTH.equals(statKey);
        }
        return changedMaxHealth;
    }

    private OptionalDouble workingValue(LivingEntity entity, String statKey, Map<String, Double> workingValues) {
        Double value = workingValues.get(statKey);
        if (value != null) {
            return OptionalDouble.of(value);
        }

        Map<String, Double> baselines = baselineStore.baselines(entity);
        value = baselines.get(statKey);
        if (value != null) {
            workingValues.put(statKey, value);
            return OptionalDouble.of(value);
        }

        OptionalDouble currentValue = attributeAccessor.baseValue(entity, statKey);
        currentValue.ifPresent(current -> {
            baselineStore.storeBaseline(entity, statKey, current);
            workingValues.put(statKey, current);
        });
        return currentValue;
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

    private static final class MemoryStatBaselineStore implements StatBaselineStore {
        private final Map<LivingEntity, Map<String, Double>> baselines = new WeakHashMap<>();

        @Override
        public Map<String, Double> baselines(LivingEntity entity) {
            return baselines.getOrDefault(entity, Map.of());
        }

        @Override
        public void storeBaseline(LivingEntity entity, String statKey, double value) {
            baselines.computeIfAbsent(entity, ignored -> new HashMap<>()).putIfAbsent(statKey, value);
        }

        @Override
        public void clear(LivingEntity entity) {
            baselines.remove(entity);
        }
    }
}
