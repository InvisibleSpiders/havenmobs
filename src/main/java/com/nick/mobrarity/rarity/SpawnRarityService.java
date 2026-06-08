package com.nick.mobrarity.rarity;

import java.util.List;
import java.util.Optional;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

public final class SpawnRarityService {
    private final VariantSelector variantSelector;

    public SpawnRarityService(VariantSelector variantSelector) {
        this.variantSelector = variantSelector;
    }

    public Optional<MobVariant> selectVariant(MobProfile profile, SpawnReason reason) {
        if (!profile.allows(reason)) {
            return Optional.empty();
        }

        List<WeightedSelector.Entry<MobVariant>> entries = profile.variants().values().stream()
                .map(variant -> new WeightedSelector.Entry<>(variant, variant.weight()))
                .toList();
        return variantSelector.select(entries);
    }

    @FunctionalInterface
    public interface VariantSelector {
        Optional<MobVariant> select(List<WeightedSelector.Entry<MobVariant>> entries);
    }
}
