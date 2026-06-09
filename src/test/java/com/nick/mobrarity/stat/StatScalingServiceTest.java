package com.nick.mobrarity.stat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nick.mobrarity.config.ConfigSnapshot;
import com.nick.mobrarity.rarity.MobProfile;
import com.nick.mobrarity.rarity.MobVariant;
import com.nick.mobrarity.rarity.RarityTier;
import com.nick.mobrarity.tag.MobRarityData;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.junit.jupiter.api.Test;

final class StatScalingServiceTest {
    @Test
    void appliesTierAndVariantModifiersUsingMobLevel() {
        LivingEntity entity = mock(LivingEntity.class);
        when(entity.getType()).thenReturn(EntityType.SHEEP);
        RecordingAttributeAccessor accessor = new RecordingAttributeAccessor(Map.of(
                "max-health", 20.0,
                "attack-damage", 4.0));
        RecordingBaselineStore baselineStore = new RecordingBaselineStore();
        StatScalingService service = new StatScalingService(accessor, baselineStore);

        service.apply(snapshot(), entity, new MobRarityData("rare", "toxic_sheep", 5));

        assertThat(accessor.baseValues)
                .containsEntry("max-health", 31.0)
                .containsEntry("attack-damage", 8.0);
        assertThat(accessor.healedToMax).isTrue();
    }

    @Test
    void repeatedApplyUsesStoredOriginalBaseValuesWithoutCompounding() {
        LivingEntity entity = mock(LivingEntity.class);
        when(entity.getType()).thenReturn(EntityType.SHEEP);
        RecordingAttributeAccessor accessor = new RecordingAttributeAccessor(Map.of("max-health", 20.0));
        RecordingBaselineStore baselineStore = new RecordingBaselineStore();
        StatScalingService service = new StatScalingService(accessor, baselineStore);
        MobRarityData data = new MobRarityData("rare", "toxic_sheep", 5);

        service.apply(snapshot(), entity, data);
        service.apply(snapshot(), entity, data);

        assertThat(accessor.baseValues).containsEntry("max-health", 31.0);
    }

    @Test
    void clearRestoresStoredOriginalBaseValues() {
        LivingEntity entity = mock(LivingEntity.class);
        when(entity.getType()).thenReturn(EntityType.SHEEP);
        RecordingAttributeAccessor accessor = new RecordingAttributeAccessor(Map.of("max-health", 20.0));
        RecordingBaselineStore baselineStore = new RecordingBaselineStore();
        StatScalingService service = new StatScalingService(accessor, baselineStore);

        service.apply(snapshot(), entity, new MobRarityData("rare", "toxic_sheep", 5));
        service.clear(entity);

        assertThat(accessor.baseValues).containsEntry("max-health", 20.0);
        assertThat(baselineStore.baselines(entity)).isEmpty();
    }

    @Test
    void skipsUnknownStatsAndMissingConfigEntries() {
        LivingEntity entity = mock(LivingEntity.class);
        when(entity.getType()).thenReturn(EntityType.ZOMBIE);
        RecordingAttributeAccessor accessor = new RecordingAttributeAccessor(Map.of("max-health", 20.0));
        StatScalingService service = new StatScalingService(accessor);

        service.apply(snapshot(), entity, new MobRarityData("missing", "missing", 2));

        assertThat(accessor.baseValues).containsEntry("max-health", 20.0);
        assertThat(accessor.healedToMax).isFalse();
    }

    private static ConfigSnapshot snapshot() {
        RarityTier tier = new RarityTier(
                "rare",
                1.0,
                "Rare",
                Map.of(
                        "max-health", new StatModifier(4.0, 1.0, 1.0),
                        "attack-damage", new StatModifier(0.0, 1.5, 0.0)),
                Map.of());
        MobVariant variant = new MobVariant(
                "toxic_sheep",
                "rare",
                1.0,
                null,
                Map.of(
                        "max-health", new StatModifier(2.0, 1.0, 0.0),
                        "attack-damage", new StatModifier(2.0, 1.0, 0.0)),
                Map.of());
        MobProfile profile = new MobProfile(Map.of(SpawnReason.NATURAL, true), Map.of(variant.key(), variant));
        return new ConfigSnapshot(Map.of(tier.key(), tier), Map.of(EntityType.SHEEP, profile));
    }

    private static final class RecordingAttributeAccessor implements StatAttributeAccessor {
        private final Map<String, Double> baseValues = new HashMap<>();
        private boolean healedToMax;

        private RecordingAttributeAccessor(Map<String, Double> baseValues) {
            this.baseValues.putAll(baseValues);
        }

        @Override
        public OptionalDouble baseValue(LivingEntity entity, String statKey) {
            Double value = baseValues.get(statKey);
            return value == null ? OptionalDouble.empty() : OptionalDouble.of(value);
        }

        @Override
        public void setBaseValue(LivingEntity entity, String statKey, double value) {
            baseValues.put(statKey, value);
        }

        @Override
        public void healToMax(LivingEntity entity) {
            healedToMax = true;
        }
    }

    private static final class RecordingBaselineStore implements StatBaselineStore {
        private final Map<LivingEntity, Map<String, Double>> baselines = new HashMap<>();

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
