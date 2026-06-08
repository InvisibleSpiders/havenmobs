package com.nick.mobrarity.integration;

import com.nick.mobrarity.command.TargetResolver;
import com.nick.mobrarity.config.ConfigSnapshot;
import com.nick.mobrarity.tag.MobTagService;
import com.nick.mobrarity.visual.MobPlaceholderValues;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class MobRarityPlaceholderExpansion extends PlaceholderExpansion {
    private final Supplier<ConfigSnapshot> configSupplier;
    private final MobTagService mobTagService;
    private final TargetResolver targetResolver;
    private final String version;

    public MobRarityPlaceholderExpansion(
            Supplier<ConfigSnapshot> configSupplier,
            MobTagService mobTagService,
            TargetResolver targetResolver,
            String version) {
        this.configSupplier = Objects.requireNonNull(configSupplier, "configSupplier");
        this.mobTagService = Objects.requireNonNull(mobTagService, "mobTagService");
        this.targetResolver = Objects.requireNonNull(targetResolver, "targetResolver");
        this.version = Objects.requireNonNull(version, "version");
    }

    @Override
    public @NotNull String getIdentifier() {
        return "mobrarity";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Nick";
    }

    @Override
    public @NotNull String getVersion() {
        return version;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null || params.isBlank()) {
            return "";
        }
        Optional<LivingEntity> target = targetResolver.target(player);
        if (target.isEmpty()) {
            return "";
        }
        return mobTagService.read(target.get())
                .map(data -> MobPlaceholderValues.from(configSupplier.get(), target.get(), data))
                .map(values -> value(values, params))
                .orElse("");
    }

    private static String value(Map<String, String> values, String params) {
        return values.getOrDefault("mobrarity_" + params.toLowerCase(java.util.Locale.ROOT), "");
    }
}
