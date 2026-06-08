package com.nick.mobrarity.effect;

import com.nick.mobrarity.config.ConfigSnapshot;
import com.nick.mobrarity.rarity.MobProfile;
import com.nick.mobrarity.rarity.MobVariant;
import com.nick.mobrarity.rarity.RarityTier;
import com.nick.mobrarity.tag.MobRarityData;
import com.nick.mobrarity.tag.MobTagService;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import org.bukkit.entity.LivingEntity;

public final class RarityTriggerService {
    private final Supplier<ConfigSnapshot> configSupplier;
    private final MobTagService mobTagService;

    public RarityTriggerService(Supplier<ConfigSnapshot> configSupplier, MobTagService mobTagService) {
        this.configSupplier = Objects.requireNonNull(configSupplier, "configSupplier");
        this.mobTagService = Objects.requireNonNull(mobTagService, "mobTagService");
    }

    public Optional<TriggerDefinition> trigger(LivingEntity entity, String triggerKey) {
        Optional<MobRarityData> data = mobTagService.read(entity);
        if (data.isEmpty()) {
            return Optional.empty();
        }

        ConfigSnapshot snapshot = configSupplier.get();
        MobProfile profile = snapshot.mobProfiles().get(entity.getType());
        if (profile == null) {
            return Optional.empty();
        }

        MobVariant variant = profile.variants().get(data.get().variantKey());
        if (variant == null) {
            return Optional.empty();
        }

        TriggerDefinition variantTrigger = variant.triggers().get(triggerKey);
        if (variantTrigger != null) {
            return Optional.of(variantTrigger);
        }

        RarityTier tier = snapshot.tiers().get(data.get().tierKey());
        if (tier == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(tier.triggers().get(triggerKey));
    }
}
