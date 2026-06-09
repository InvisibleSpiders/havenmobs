package com.nick.mobrarity.command;

import com.nick.mobrarity.config.ConfigSnapshot;
import com.nick.mobrarity.rarity.MobProfile;
import com.nick.mobrarity.rarity.MobVariant;
import com.nick.mobrarity.stat.StatScalingService;
import com.nick.mobrarity.tag.MobRarityData;
import com.nick.mobrarity.tag.MobTagService;
import com.nick.mobrarity.visual.DisplayMode;
import com.nick.mobrarity.visual.VisualService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.function.Supplier;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public final class MobRarityAdminService implements MobRarityCommand.AdminService {
    private final Supplier<ConfigSnapshot> configSupplier;
    private final MobTagService mobTagService;
    private final TargetResolver targetResolver;
    private final StatScalingService statScalingService;
    private final VisualService visualService;

    public MobRarityAdminService(
            Supplier<ConfigSnapshot> configSupplier,
            MobTagService mobTagService,
            TargetResolver targetResolver) {
        this(configSupplier, mobTagService, targetResolver, null);
    }

    public MobRarityAdminService(
            Supplier<ConfigSnapshot> configSupplier,
            MobTagService mobTagService,
            TargetResolver targetResolver,
            StatScalingService statScalingService) {
        this(configSupplier, mobTagService, targetResolver, statScalingService, null);
    }

    public MobRarityAdminService(
            Supplier<ConfigSnapshot> configSupplier,
            MobTagService mobTagService,
            TargetResolver targetResolver,
            StatScalingService statScalingService,
            VisualService visualService) {
        this.configSupplier = Objects.requireNonNull(configSupplier, "configSupplier");
        this.mobTagService = Objects.requireNonNull(mobTagService, "mobTagService");
        this.targetResolver = Objects.requireNonNull(targetResolver, "targetResolver");
        this.statScalingService = statScalingService;
        this.visualService = visualService;
    }

    @Override
    public AdminCommandResult inspect(Player player) {
        return targetResolver.target(player)
                .map(target -> mobTagService.read(target)
                        .map(data -> AdminCommandResult.success("Target: %s %s/%s level %d.".formatted(
                                target.getType().name(), data.tierKey(), data.variantKey(), data.level())))
                        .orElseGet(() -> AdminCommandResult.failure(
                                "Target: %s has no MobRarity data.".formatted(target.getType().name()))))
                .orElseGet(() -> AdminCommandResult.failure("No targeted living mob found."));
    }

    @Override
    public AdminCommandResult set(Player player, String tierKey, String variantKey, int level) {
        return targetResolver.target(player)
                .map(target -> tagEntity(target, tierKey, variantKey, level)
                        .map(data -> AdminCommandResult.success("Set %s to %s/%s level %d.".formatted(
                                target.getType().name(), data.tierKey(), data.variantKey(), data.level())))
                        .orElseGet(() -> invalidVariant(target.getType(), tierKey, variantKey)))
                .orElseGet(() -> AdminCommandResult.failure("No targeted living mob found."));
    }

    @Override
    public AdminCommandResult clear(Player player) {
        return targetResolver.target(player)
                .map(target -> {
                    clearScaling(target);
                    mobTagService.clear(target);
                    clearVisuals(target);
                    return AdminCommandResult.success(
                            "Cleared MobRarity data from %s.".formatted(target.getType().name()));
                })
                .orElseGet(() -> AdminCommandResult.failure("No targeted living mob found."));
    }

    @Override
    public AdminCommandResult spawn(
            CommandSender sender,
            Player target,
            EntityType entityType,
            String tierKey,
            String variantKey,
            int level) {
        if (!entityType.isAlive()) {
            return AdminCommandResult.failure("%s is not a living mob type.".formatted(entityType.name()));
        }
        if (configuredVariant(entityType, tierKey, variantKey).isEmpty()) {
            return invalidVariant(entityType, tierKey, variantKey);
        }

        Location location = target.getLocation();
        World world = location.getWorld();
        if (world == null) {
            return AdminCommandResult.failure("Target player has no world.");
        }

        Entity spawned = world.spawnEntity(location, entityType);
        if (!(spawned instanceof LivingEntity livingEntity)) {
            return AdminCommandResult.failure("Spawned entity was not a living mob.");
        }

        mobTagService.tag(livingEntity, new MobRarityData(tierKey, variantKey, level));
        applyScaling(livingEntity, new MobRarityData(tierKey, variantKey, level));
        applyVisuals(livingEntity, new MobRarityData(tierKey, variantKey, level));
        return AdminCommandResult.success("Spawned %s %s/%s level %d on %s.".formatted(
                entityType.name(), tierKey, variantKey, level, target.getName()));
    }

    @Override
    public Map<String, List<String>> completions() {
        ConfigSnapshot snapshot = configSupplier.get();
        TreeSet<String> tiers = new TreeSet<>(snapshot.tiers().keySet());
        TreeSet<String> variants = new TreeSet<>();
        TreeSet<String> mobs = new TreeSet<>();
        snapshot.mobProfiles().keySet().forEach(entityType -> mobs.add(entityType.name()));
        for (MobProfile profile : snapshot.mobProfiles().values()) {
            variants.addAll(profile.variants().keySet());
        }
        return Map.of(
                "tiers", new ArrayList<>(tiers),
                "variants", new ArrayList<>(variants),
                "mobs", new ArrayList<>(mobs));
    }

    @Override
    public AdminCommandResult list(String category) {
        return switch (category.toLowerCase(java.util.Locale.ROOT)) {
            case "tiers" -> AdminCommandResult.success("Tiers: " + joined(completions().get("tiers")));
            case "variants" -> AdminCommandResult.success("Variants: " + joined(completions().get("variants")));
            case "mobs" -> AdminCommandResult.success("Mobs: " + joined(completions().get("mobs")));
            default -> AdminCommandResult.failure(
                    "Unknown list category '%s'. Use tiers, variants, or mobs.".formatted(category));
        };
    }

    private java.util.Optional<MobRarityData> tagEntity(
            LivingEntity entity,
            String tierKey,
            String variantKey,
            int level) {
        if (configuredVariant(entity.getType(), tierKey, variantKey).isEmpty()) {
            return java.util.Optional.empty();
        }

        MobRarityData data = new MobRarityData(tierKey, variantKey, level);
        mobTagService.tag(entity, data);
        applyScaling(entity, data);
        applyVisuals(entity, data);
        return java.util.Optional.of(data);
    }

    private void applyScaling(LivingEntity entity, MobRarityData data) {
        if (statScalingService != null) {
            statScalingService.apply(configSupplier.get(), entity, data);
        }
    }

    private void clearScaling(LivingEntity entity) {
        if (statScalingService != null) {
            statScalingService.clear(entity);
        }
    }

    private void applyVisuals(LivingEntity entity, MobRarityData data) {
        if (visualService != null) {
            visualService.applyNametag(configSupplier.get(), entity, data, DisplayMode.TARGETED);
        }
    }

    private void clearVisuals(LivingEntity entity) {
        if (visualService != null) {
            visualService.clearNametag(entity);
        }
    }

    private java.util.Optional<MobVariant> configuredVariant(EntityType entityType, String tierKey, String variantKey) {
        ConfigSnapshot snapshot = configSupplier.get();
        if (!snapshot.tiers().containsKey(tierKey)) {
            return java.util.Optional.empty();
        }

        MobProfile profile = snapshot.mobProfiles().get(entityType);
        if (profile == null) {
            return java.util.Optional.empty();
        }

        MobVariant variant = profile.variants().get(variantKey);
        if (variant == null || !variant.tierKey().equals(tierKey)) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(variant);
    }

    private static AdminCommandResult invalidVariant(EntityType entityType, String tierKey, String variantKey) {
        return AdminCommandResult.failure(
                "Variant '%s' is not configured for %s.".formatted(variantKey, entityType.name()));
    }

    private static String joined(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "(none)";
        }
        return String.join(", ", values);
    }
}
