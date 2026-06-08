package com.nick.mobrarity.effect;

import com.nick.mobrarity.integration.EconomyAdapter;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.random.RandomGenerator;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

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
        Objects.requireNonNull(economyAdapter, "economyAdapter");
        Objects.requireNonNull(random, "random");
        Objects.requireNonNull(commandDispatcher, "commandDispatcher");
        Objects.requireNonNull(itemDropper, "itemDropper");
        Objects.requireNonNull(potionApplier, "potionApplier");
        this.actions = Map.of(
                "item_drop", (action, context) -> dropItem(action, context, random, itemDropper),
                "potion_effect", (action, context) -> applyPotionEffect(action, context, potionApplier),
                "currency_drop", (action, context) -> dropCurrency(action, context, economyAdapter),
                "command", (action, context) -> runCommand(action, context, commandDispatcher),
                "hostile_target", BukkitEffectActionRegistry::makeHostile);
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
            CommandDispatcher commandDispatcher) {
        String command = stringValue(action.values().get("command"))
                .map(value -> replacePlaceholders(value, context))
                .orElse("");
        if (command.isBlank()) {
            return;
        }
        String source = stringValue(action.values().getOrDefault("as", "console"))
                .orElse("console")
                .toLowerCase(Locale.ROOT);
        if ("player".equals(source)) {
            context.player().ifPresent(player -> commandDispatcher.dispatchPlayer(player, command));
            return;
        }
        commandDispatcher.dispatchConsole(command);
    }

    private static void makeHostile(ActionDefinition action, TriggerContext context) {
        if (context.entity().orElse(null) instanceof Mob mob && context.player().isPresent()) {
            mob.setTarget(context.player().get());
        }
    }

    private static String replacePlaceholders(String value, TriggerContext context) {
        String result = value;
        if (context.player().isPresent()) {
            result = result.replace("%player%", context.player().get().getName());
        }
        if (context.entity().isPresent()) {
            result = result.replace("%entity_type%", context.entity().get().getType().name());
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
}
