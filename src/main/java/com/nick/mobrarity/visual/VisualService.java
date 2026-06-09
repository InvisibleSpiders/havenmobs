package com.nick.mobrarity.visual;

import com.nick.mobrarity.config.ConfigSnapshot;
import com.nick.mobrarity.rarity.MobProfile;
import com.nick.mobrarity.rarity.MobVariant;
import com.nick.mobrarity.rarity.RarityTier;
import com.nick.mobrarity.tag.MobRarityData;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.LivingEntity;

public final class VisualService {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final MobTextFormatter textFormatter;

    public VisualService(MobTextFormatter textFormatter) {
        this.textFormatter = Objects.requireNonNull(textFormatter, "textFormatter");
    }

    public void applyNametag(ConfigSnapshot snapshot, LivingEntity entity, MobRarityData data, DisplayMode mode) {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(data, "data");
        Objects.requireNonNull(mode, "mode");

        if (mode == DisplayMode.DISABLED) {
            entity.customName(null);
            entity.setCustomNameVisible(false);
            return;
        }

        Optional<String> template = nametagTemplate(snapshot, entity, data);
        if (template.isEmpty()) {
            return;
        }

        Map<String, String> values = MobPlaceholderValues.from(snapshot, entity, data);
        String rendered = textFormatter.formatPlain(template.get(), values);
        entity.customName(MINI_MESSAGE.deserialize(rendered));
        entity.setCustomNameVisible(mode == DisplayMode.ALWAYS);
    }

    public void clearNametag(LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");

        entity.customName(null);
        entity.setCustomNameVisible(false);
    }

    private static Optional<String> nametagTemplate(ConfigSnapshot snapshot, LivingEntity entity, MobRarityData data) {
        MobProfile profile = snapshot.mobProfiles().get(entity.getType());
        if (profile != null) {
            MobVariant variant = profile.variants().get(data.variantKey());
            if (variant != null && variant.nametag() != null && !variant.nametag().isBlank()) {
                return Optional.of(variant.nametag());
            }
        }

        RarityTier tier = snapshot.tiers().get(data.tierKey());
        if (tier != null && tier.nametag() != null && !tier.nametag().isBlank()) {
            return Optional.of(tier.nametag());
        }
        return Optional.empty();
    }
}
