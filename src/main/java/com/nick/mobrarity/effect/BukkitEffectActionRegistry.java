package com.nick.mobrarity.effect;

import com.nick.mobrarity.integration.EconomyAdapter;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public final class BukkitEffectActionRegistry implements EffectActionRegistry {
    private final Map<String, EffectAction> actions;

    public BukkitEffectActionRegistry(EconomyAdapter economyAdapter, RandomGenerator random) {
        this(economyAdapter, random, new BukkitCommandDispatcher());
    }

    BukkitEffectActionRegistry(
            EconomyAdapter economyAdapter,
            RandomGenerator random,
            CommandDispatcher commandDispatcher) {
        this(economyAdapter, random, commandDispatcher, new BukkitItemDropper(), new BukkitPotionApplier());
    }

    BukkitEffectActionRegistry(
            EconomyAdapter economyAdapter,
            RandomGenerator random,
            CommandDispatcher commandDispatcher,
            ItemDropper itemDropper,
            PotionApplier potionApplier) {
        this(
                economyAdapter,
                random,
                commandDispatcher,
                itemDropper,
                potionApplier,
                new BukkitExperienceDropper(),
                new BukkitHealthEditor(),
                new BukkitDamageDealer(),
                new BukkitVelocityApplier(),
                new BukkitWorldEffects(),
                new BukkitLootTableDropper());
    }

    BukkitEffectActionRegistry(
            EconomyAdapter economyAdapter,
            RandomGenerator random,
            CommandDispatcher commandDispatcher,
            ItemDropper itemDropper,
            PotionApplier potionApplier,
            ExperienceDropper experienceDropper,
            HealthEditor healthEditor,
            DamageDealer damageDealer,
            VelocityApplier velocityApplier,
            WorldEffects worldEffects,
            LootTableDropper lootTableDropper) {
        Objects.requireNonNull(economyAdapter, "economyAdapter");
        Objects.requireNonNull(random, "random");
        Objects.requireNonNull(commandDispatcher, "commandDispatcher");
        Objects.requireNonNull(itemDropper, "itemDropper");
        Objects.requireNonNull(potionApplier, "potionApplier");
        Objects.requireNonNull(experienceDropper, "experienceDropper");
        Objects.requireNonNull(healthEditor, "healthEditor");
        Objects.requireNonNull(damageDealer, "damageDealer");
        Objects.requireNonNull(velocityApplier, "velocityApplier");
        Objects.requireNonNull(worldEffects, "worldEffects");
        Objects.requireNonNull(lootTableDropper, "lootTableDropper");
        this.actions = Map.ofEntries(
                Map.entry("item_drop", (action, context) -> dropItem(action, context, random, itemDropper)),
                Map.entry("potion_effect", (action, context) -> applyPotionEffect(action, context, potionApplier)),
                Map.entry("currency_drop", (action, context) -> dropCurrency(action, context, economyAdapter)),
                Map.entry("loot_table", (action, context) -> dropLootTable(action, context, lootTableDropper)),
                Map.entry("console_command", (action, context) -> runCommand(action, context, commandDispatcher, "console")),
                Map.entry("player_command", (action, context) -> runCommand(action, context, commandDispatcher, "player")),
                Map.entry("xp_drop", (action, context) -> dropExperience(action, context, random, experienceDropper)),
                Map.entry("heal", (action, context) -> heal(action, context, healthEditor)),
                Map.entry("damage", (action, context) -> damage(action, context, damageDealer)),
                Map.entry("knockback", (action, context) -> knockback(action, context, velocityApplier)),
                Map.entry("lightning_effect", (action, context) -> strikeLightningEffect(context, worldEffects)),
                Map.entry("hostile_target", BukkitEffectActionRegistry::makeHostile));
    }

    BukkitEffectActionRegistry(
            EconomyAdapter economyAdapter,
            RandomGenerator random,
            CommandDispatcher commandDispatcher,
            ItemDropper itemDropper,
            PotionApplier potionApplier,
            ExperienceDropper experienceDropper,
            HealthEditor healthEditor,
            DamageDealer damageDealer,
            VelocityApplier velocityApplier,
            WorldEffects worldEffects) {
        this(
                economyAdapter,
                random,
                commandDispatcher,
                itemDropper,
                potionApplier,
                experienceDropper,
                healthEditor,
                damageDealer,
                velocityApplier,
                worldEffects,
                new BukkitLootTableDropper());
    }

    @Override
    public Optional<EffectAction> action(String type) {
        return Optional.ofNullable(actions.get(type));
    }

    private static void dropItem(
            ActionDefinition action,
            TriggerContext context,
            RandomGenerator random,
            ItemDropper itemDropper) {
        Optional<LivingEntity> entity = context.entity();
        if (entity.isEmpty()) {
            return;
        }
        Material material = material(action.values().get("material")).orElse(null);
        if (material == null) {
            return;
        }
        int amount = amount(action.values().getOrDefault("amount", 1), random);
        if (amount < 1) {
            return;
        }
        Location location = entity.get().getLocation();
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        itemDropper.drop(world, location, material, amount);
    }

    private static void applyPotionEffect(
            ActionDefinition action,
            TriggerContext context,
            PotionApplier potionApplier) {
        Optional<String> effectName = stringValue(action.values().get("effect"));
        if (effectName.isEmpty()) {
            return;
        }
        int durationTicks = integer(action.values().getOrDefault("duration-ticks", 100)).orElse(100);
        int amplifier = integer(action.values().getOrDefault("amplifier", 0)).orElse(0);
        if (durationTicks < 1 || amplifier < 0) {
            return;
        }
        String target = stringValue(action.values().getOrDefault("target", "player"))
                .orElse("player")
                .toLowerCase(Locale.ROOT);
        if ("mob".equals(target)) {
            context.entity().ifPresent(entity -> potionApplier.apply(entity, effectName.get(), durationTicks, amplifier));
            return;
        }
        context.player().ifPresent(player -> potionApplier.apply(player, effectName.get(), durationTicks, amplifier));
    }

    private static void dropCurrency(
            ActionDefinition action,
            TriggerContext context,
            EconomyAdapter economyAdapter) {
        if (!economyAdapter.available()) {
            return;
        }
        double amount = decimal(action.values().getOrDefault("amount", 0)).orElse(0.0);
        if (amount <= 0 || !Double.isFinite(amount)) {
            return;
        }
        context.player().ifPresent(player -> economyAdapter.deposit(player, amount));
    }

    private static void runCommand(
            ActionDefinition action,
            TriggerContext context,
            CommandDispatcher commandDispatcher,
            String source) {
        String command = stringValue(action.values().get("command"))
                .map(value -> replacePlaceholders(value, context))
                .orElse("");
        if (command.isBlank()) {
            return;
        }
        switch (source.toLowerCase(Locale.ROOT)) {
            case "player" -> context.player().ifPresent(player -> commandDispatcher.dispatchPlayer(player, command));
            default -> commandDispatcher.dispatchConsole(command);
        }
    }

    private static void dropExperience(
            ActionDefinition action,
            TriggerContext context,
            RandomGenerator random,
            ExperienceDropper experienceDropper) {
        Optional<LivingEntity> entity = context.entity();
        if (entity.isEmpty()) {
            return;
        }
        int amount = amount(action.values().getOrDefault("amount", 0), random);
        if (amount < 1) {
            return;
        }
        Location location = entity.get().getLocation();
        World world = location.getWorld();
        if (world != null) {
            experienceDropper.drop(world, location, amount);
        }
    }

    private static void dropLootTable(
            ActionDefinition action,
            TriggerContext context,
            LootTableDropper lootTableDropper) {
        Optional<LivingEntity> entity = context.entity();
        Optional<NamespacedKey> tableKey = namespacedKey(action.values().get("table"));
        if (entity.isEmpty() || tableKey.isEmpty()) {
            return;
        }

        int rolls = integer(action.values().getOrDefault("rolls", 1)).orElse(1);
        if (rolls < 1) {
            return;
        }

        Location location = entity.get().getLocation();
        if (location.getWorld() != null) {
            lootTableDropper.drop(location, tableKey.get(), entity.get(), context.player().orElse(null), rolls);
        }
    }

    private static void heal(
            ActionDefinition action,
            TriggerContext context,
            HealthEditor healthEditor) {
        Optional<LivingEntity> target = target(action, context);
        double amount = decimal(action.values().getOrDefault("amount", 0)).orElse(0.0);
        if (target.isEmpty() || amount <= 0 || !Double.isFinite(amount)) {
            return;
        }
        LivingEntity entity = target.get();
        double health = Math.min(healthEditor.maxHealth(entity), healthEditor.health(entity) + amount);
        healthEditor.setHealth(entity, health);
    }

    private static void damage(
            ActionDefinition action,
            TriggerContext context,
            DamageDealer damageDealer) {
        Optional<LivingEntity> target = target(action, context);
        double amount = decimal(action.values().getOrDefault("amount", 0)).orElse(0.0);
        if (target.isEmpty() || amount <= 0 || !Double.isFinite(amount)) {
            return;
        }
        damageDealer.damage(target.get(), amount, context.entity().orElse(null));
    }

    private static void knockback(
            ActionDefinition action,
            TriggerContext context,
            VelocityApplier velocityApplier) {
        Optional<LivingEntity> target = target(action, context);
        Optional<LivingEntity> source = context.entity();
        double strength = decimal(action.values().getOrDefault("strength", 1.0)).orElse(1.0);
        double y = decimal(action.values().getOrDefault("y", 0.35)).orElse(0.35);
        if (target.isEmpty() || source.isEmpty() || strength <= 0 || !Double.isFinite(strength)) {
            return;
        }
        Vector direction = target.get().getLocation().toVector()
                .subtract(source.get().getLocation().toVector());
        if (direction.lengthSquared() == 0) {
            direction = new Vector(1, 0, 0);
        }
        Vector velocity = direction.normalize().multiply(strength).setY(y);
        velocityApplier.setVelocity(target.get(), velocity);
    }

    private static void strikeLightningEffect(TriggerContext context, WorldEffects worldEffects) {
        Optional<LivingEntity> entity = context.entity();
        if (entity.isEmpty()) {
            return;
        }
        Location location = entity.get().getLocation();
        World world = location.getWorld();
        if (world != null) {
            worldEffects.strikeLightningEffect(world, location);
        }
    }

    private static void makeHostile(ActionDefinition action, TriggerContext context) {
        if (context.entity().orElse(null) instanceof Mob mob && context.player().isPresent()) {
            mob.setTarget(context.player().get());
        }
    }

    private static Optional<LivingEntity> target(ActionDefinition action, TriggerContext context) {
        String target = stringValue(action.values().getOrDefault("target", "player"))
                .orElse("player")
                .toLowerCase(Locale.ROOT);
        if ("mob".equals(target)) {
            return context.entity();
        }
        return context.player().map(player -> (LivingEntity) player);
    }

    private static String replacePlaceholders(String value, TriggerContext context) {
        String result = value;
        Optional<String> playerName = context.player().map(Player::getName);
        if (playerName.isPresent()) {
            result = result.replace("%player%", playerName.get());
        }
        Optional<String> entityType = context.entity().map(LivingEntity::getType).map(EntityType::name);
        if (entityType.isPresent()) {
            result = result.replace("%entity_type%", entityType.get());
        }
        return result;
    }

    private static Optional<Material> material(Object value) {
        return stringValue(value).flatMap(materialName -> {
            try {
                return Optional.of(Material.valueOf(materialName.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException exception) {
                return Optional.empty();
            }
        });
    }

    private static Optional<NamespacedKey> namespacedKey(Object value) {
        Optional<String> text = stringValue(value);
        if (text.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(NamespacedKey.fromString(text.get()));
    }

    private static int amount(Object value, RandomGenerator random) {
        Optional<Integer> direct = integer(value);
        if (direct.isPresent()) {
            return direct.get();
        }
        Optional<String> range = stringValue(value);
        if (range.isEmpty() || !range.get().contains("-")) {
            return 0;
        }
        String[] parts = range.get().split("-", 2);
        Optional<Integer> min = integer(parts[0].trim());
        Optional<Integer> max = integer(parts[1].trim());
        if (min.isEmpty() || max.isEmpty() || min.get() > max.get()) {
            return 0;
        }
        int bound = max.get() - min.get() + 1;
        return min.get() + random.nextInt(bound);
    }

    private static Optional<Integer> integer(Object value) {
        if (value instanceof Number number) {
            return Optional.of(number.intValue());
        }
        return stringValue(value).flatMap(text -> {
            try {
                return Optional.of(Integer.parseInt(text));
            } catch (NumberFormatException exception) {
                return Optional.empty();
            }
        });
    }

    private static Optional<Double> decimal(Object value) {
        if (value instanceof Number number) {
            return Optional.of(number.doubleValue());
        }
        return stringValue(value).flatMap(text -> {
            try {
                return Optional.of(Double.parseDouble(text));
            } catch (NumberFormatException exception) {
                return Optional.empty();
            }
        });
    }

    private static Optional<String> stringValue(Object value) {
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(String.valueOf(value));
    }

    interface CommandDispatcher {
        void dispatchConsole(String command);

        void dispatchPlayer(Player player, String command);
    }

    interface ItemDropper {
        void drop(World world, Location location, Material material, int amount);
    }

    interface PotionApplier {
        void apply(LivingEntity target, String effectName, int durationTicks, int amplifier);
    }

    interface ExperienceDropper {
        void drop(World world, Location location, int amount);
    }

    interface HealthEditor {
        double health(LivingEntity target);

        double maxHealth(LivingEntity target);

        void setHealth(LivingEntity target, double health);
    }

    interface DamageDealer {
        void damage(LivingEntity target, double amount, LivingEntity source);
    }

    interface VelocityApplier {
        void setVelocity(LivingEntity target, Vector velocity);
    }

    interface WorldEffects {
        void strikeLightningEffect(World world, Location location);
    }

    interface LootTableDropper {
        void drop(Location location, NamespacedKey tableKey, LivingEntity entity, Player player, int rolls);
    }

    private static final class BukkitCommandDispatcher implements CommandDispatcher {
        @Override
        public void dispatchConsole(String command) {
            ConsoleCommandSender console = Bukkit.getConsoleSender();
            Bukkit.dispatchCommand(console, command);
        }

        @Override
        public void dispatchPlayer(Player player, String command) {
            player.performCommand(command);
        }
    }

    private static final class BukkitItemDropper implements ItemDropper {
        @Override
        public void drop(World world, Location location, Material material, int amount) {
            world.dropItemNaturally(location, new ItemStack(material, amount));
        }
    }

    private static final class BukkitPotionApplier implements PotionApplier {
        @Override
        public void apply(LivingEntity target, String effectName, int durationTicks, int amplifier) {
            PotionEffectType type = PotionEffectType.getByName(effectName);
            if (type != null) {
                target.addPotionEffect(new PotionEffect(type, durationTicks, amplifier));
            }
        }
    }

    private static final class BukkitExperienceDropper implements ExperienceDropper {
        @Override
        public void drop(World world, Location location, int amount) {
            world.spawn(location, ExperienceOrb.class, orb -> orb.setExperience(amount));
        }
    }

    private static final class BukkitHealthEditor implements HealthEditor {
        @Override
        public double health(LivingEntity target) {
            return target.getHealth();
        }

        @Override
        public double maxHealth(LivingEntity target) {
            return target.getMaxHealth();
        }

        @Override
        public void setHealth(LivingEntity target, double health) {
            target.setHealth(health);
        }
    }

    private static final class BukkitDamageDealer implements DamageDealer {
        @Override
        public void damage(LivingEntity target, double amount, LivingEntity source) {
            if (source == null) {
                target.damage(amount);
                return;
            }
            target.damage(amount, source);
        }
    }

    private static final class BukkitVelocityApplier implements VelocityApplier {
        @Override
        public void setVelocity(LivingEntity target, Vector velocity) {
            target.setVelocity(velocity);
        }
    }

    private static final class BukkitWorldEffects implements WorldEffects {
        @Override
        public void strikeLightningEffect(World world, Location location) {
            world.strikeLightningEffect(location);
        }
    }

    private static final class BukkitLootTableDropper implements LootTableDropper {
        @Override
        public void drop(Location location, NamespacedKey tableKey, LivingEntity entity, Player player, int rolls) {
            LootTable lootTable = Bukkit.getLootTable(tableKey);
            if (lootTable == null || location.getWorld() == null) {
                return;
            }

            LootContext.Builder contextBuilder = new LootContext.Builder(location)
                    .lootedEntity(entity);
            if (player != null) {
                contextBuilder.killer(player);
            }
            LootContext lootContext = contextBuilder.build();
            for (int roll = 0; roll < rolls; roll++) {
                for (ItemStack item : lootTable.populateLoot(ThreadLocalRandom.current(), lootContext)) {
                    location.getWorld().dropItemNaturally(location, item);
                }
            }
        }
    }
}
