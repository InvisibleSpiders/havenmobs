package com.nick.mobrarity.command;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;

public final class MobRarityTabCompleter implements TabCompleter {
    private static final List<String> BASE_SUBCOMMANDS = List.of("reload", "validate", "list", "inspect", "set", "spawn", "clear");
    private static final List<String> LIST_CATEGORIES = List.of("tiers", "variants", "mobs");
    private final Supplier<Map<String, List<String>>> completionSupplier;
    private final Supplier<List<String>> playerNameSupplier;

    public MobRarityTabCompleter() {
        this(Map::of, List::of);
    }

    public MobRarityTabCompleter(
            Supplier<Map<String, List<String>>> completionSupplier,
            Supplier<List<String>> playerNameSupplier) {
        this.completionSupplier = Objects.requireNonNull(completionSupplier, "completionSupplier");
        this.playerNameSupplier = Objects.requireNonNull(playerNameSupplier, "playerNameSupplier");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args == null || args.length <= 1) {
            return filter(BASE_SUBCOMMANDS, prefix(args, 0));
        }

        String subcommand = args[0] == null ? "" : args[0].toLowerCase(Locale.ROOT);
        return switch (subcommand) {
            case "list" -> completeList(args);
            case "set" -> completeSet(args);
            case "spawn" -> completeSpawn(args);
            default -> List.of();
        };
    }

    private List<String> completeList(String[] args) {
        return args.length == 2 ? filter(LIST_CATEGORIES, prefix(args, 1)) : List.of();
    }

    private List<String> completeSet(String[] args) {
        Map<String, List<String>> completions = completionSupplier.get();
        return switch (args.length) {
            case 2 -> filter(completions.getOrDefault("tiers", List.of()), prefix(args, 1));
            case 3 -> filter(completions.getOrDefault("variants", List.of()), prefix(args, 2));
            default -> List.of();
        };
    }

    private List<String> completeSpawn(String[] args) {
        Map<String, List<String>> completions = completionSupplier.get();
        return switch (args.length) {
            case 2 -> filter(livingEntityTypes(), prefix(args, 1));
            case 3 -> filter(completions.getOrDefault("tiers", List.of()), prefix(args, 2));
            case 4 -> filter(completions.getOrDefault("variants", List.of()), prefix(args, 3));
            case 6 -> filter(playerNameSupplier.get(), prefix(args, 5));
            default -> List.of();
        };
    }

    private static List<String> livingEntityTypes() {
        return Arrays.stream(EntityType.values())
                .filter(EntityType::isAlive)
                .map(EntityType::name)
                .sorted()
                .toList();
    }

    private static String prefix(String[] args, int index) {
        if (args == null || args.length <= index || args[index] == null) {
            return "";
        }
        return args[index].toLowerCase(Locale.ROOT);
    }

    private static List<String> filter(List<String> values, String prefix) {
        return values.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix))
                .toList();
    }
}
