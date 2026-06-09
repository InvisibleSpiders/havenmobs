package com.nick.mobrarity.visual;

import com.nick.mobrarity.config.ConfigSnapshot;
import com.nick.mobrarity.rarity.MobProfile;
import com.nick.mobrarity.rarity.MobVariant;
import com.nick.mobrarity.rarity.RarityTier;
import com.nick.mobrarity.tag.MobRarityData;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.LivingEntity;

public final class MobPlaceholderValues {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();

    private MobPlaceholderValues() {
    }

    public static Map<String, String> from(ConfigSnapshot snapshot, LivingEntity entity, MobRarityData data) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("mobrarity_tier", tierDisplay(snapshot, data));
        values.put("mobrarity_tier_key", data.tierKey());
        values.put("mobrarity_variant", variantDisplay(snapshot, entity, data));
        values.put("mobrarity_variant_key", data.variantKey());
        values.put("mobrarity_level", String.valueOf(data.level()));
        values.put("mobrarity_entity", humanize(entity.getType().name()));
        return values;
    }

    private static String tierDisplay(ConfigSnapshot snapshot, MobRarityData data) {
        RarityTier tier = snapshot.tiers().get(data.tierKey());
        if (tier == null || tier.displayName() == null || tier.displayName().isBlank()) {
            return humanize(data.tierKey());
        }
        return plainMiniMessage(tier.displayName());
    }

    private static String variantDisplay(ConfigSnapshot snapshot, LivingEntity entity, MobRarityData data) {
        MobProfile profile = snapshot.mobProfiles().get(entity.getType());
        if (profile == null) {
            return humanize(data.variantKey());
        }
        MobVariant variant = profile.variants().get(data.variantKey());
        if (variant == null) {
            return humanize(data.variantKey());
        }
        if (variant.nametag() == null || variant.nametag().isBlank()) {
            return humanize(variant.key());
        }
        return plainMiniMessage(variant.nametag());
    }

    private static String plainMiniMessage(String value) {
        try {
            return PLAIN_TEXT.serialize(MINI_MESSAGE.deserialize(value));
        } catch (RuntimeException exception) {
            return value;
        }
    }

    private static String humanize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String[] parts = value.toLowerCase(Locale.ROOT).split("[_\\s-]+");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                result.append(part.substring(1));
            }
        }
        return result.toString();
    }
}
