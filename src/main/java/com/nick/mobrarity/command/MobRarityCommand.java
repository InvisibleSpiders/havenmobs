package com.nick.mobrarity.command;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public final class MobRarityCommand implements CommandExecutor {
    static final String USAGE = "/mobrarity reload|validate|list|inspect|debug|set|spawn|clear";
    private static final String LIST_USAGE = "/mobrarity list <tiers|variants|mobs>";
    private static final String SET_USAGE = "/mobrarity set <tier> <variant> [level]";
    private static final String SPAWN_USAGE = "/mobrarity spawn <entity> <tier> <variant> [level] [player]";

    private final AdminService adminService;
    private final AdminAction reloadAction;
    private final AdminAction validateAction;
    private final PlayerResolver playerResolver;
    private final Supplier<List<String>> playerNameSupplier;

    public MobRarityCommand() {
        this(
                new UnavailableAdminService(),
                () -> AdminCommandResult.failure("MobRarity admin services are not available."),
                () -> AdminCommandResult.failure("MobRarity admin services are not available."),
                name -> Optional.empty(),
                List::of);
    }

    public MobRarityCommand(
            AdminService adminService,
            AdminAction reloadAction,
            AdminAction validateAction,
            PlayerResolver playerResolver,
            Supplier<List<String>> playerNameSupplier) {
        this.adminService = Objects.requireNonNull(adminService, "adminService");
        this.reloadAction = Objects.requireNonNull(reloadAction, "reloadAction");
        this.validateAction = Objects.requireNonNull(validateAction, "validateAction");
        this.playerResolver = Objects.requireNonNull(playerResolver, "playerResolver");
        this.playerNameSupplier = Objects.requireNonNull(playerNameSupplier, "playerNameSupplier");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender == null) {
            return true;
        }
        if (args == null || args.length == 0 || args[0] == null || args[0].isBlank()) {
            sender.sendMessage(USAGE);
            return true;
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        if (!hasPermission(sender, subcommand)) {
            sender.sendMessage("You do not have permission to use /mobrarity %s.".formatted(subcommand));
            return true;
        }

        AdminCommandResult result = switch (subcommand) {
            case "reload" -> reload(args);
            case "validate" -> validate(args);
            case "list" -> list(args);
            case "inspect" -> inspect(sender);
            case "debug" -> debug(sender);
            case "set" -> set(sender, args);
            case "spawn" -> spawn(sender, args);
            case "clear" -> clear(sender);
            default -> AdminCommandResult.failure(USAGE);
        };
        sender.sendMessage(result.message());
        return true;
    }

    private AdminCommandResult reload(String[] args) {
        if (args.length != 1) {
            return AdminCommandResult.failure("/mobrarity reload");
        }
        return reloadAction.run();
    }

    private AdminCommandResult validate(String[] args) {
        if (args.length != 1) {
            return AdminCommandResult.failure("/mobrarity validate");
        }
        return validateAction.run();
    }

    private AdminCommandResult list(String[] args) {
        if (args.length != 2) {
            return AdminCommandResult.failure(LIST_USAGE);
        }
        return adminService.list(args[1]);
    }

    private AdminCommandResult inspect(CommandSender sender) {
        return asPlayer(sender).map(adminService::inspect)
                .orElseGet(() -> AdminCommandResult.failure("Only players can inspect targeted mobs."));
    }

    private AdminCommandResult debug(CommandSender sender) {
        return asPlayer(sender).map(adminService::debug)
                .orElseGet(() -> AdminCommandResult.failure("Only players can debug targeted mobs."));
    }

    private AdminCommandResult set(CommandSender sender, String[] args) {
        if (args.length < 3 || args.length > 4) {
            return AdminCommandResult.failure(SET_USAGE);
        }
        Optional<Player> player = asPlayer(sender);
        if (player.isEmpty()) {
            return AdminCommandResult.failure("Only players can set targeted mobs.");
        }
        Optional<Integer> level = args.length == 4 ? parseLevel(args[3]) : Optional.of(1);
        if (level.isEmpty()) {
            return AdminCommandResult.failure("Level must be a positive whole number.");
        }
        return adminService.set(player.get(), args[1], args[2], level.get());
    }

    private AdminCommandResult clear(CommandSender sender) {
        return asPlayer(sender).map(adminService::clear)
                .orElseGet(() -> AdminCommandResult.failure("Only players can clear targeted mobs."));
    }

    private AdminCommandResult spawn(CommandSender sender, String[] args) {
        if (args.length < 4 || args.length > 6) {
            return AdminCommandResult.failure(SPAWN_USAGE);
        }

        Optional<EntityType> entityType = parseEntityType(args[1]);
        if (entityType.isEmpty()) {
            return AdminCommandResult.failure("Unknown entity type '%s'.".formatted(args[1]));
        }

        Optional<Integer> level = Optional.of(1);
        if (args.length >= 5) {
            level = parseLevel(args[4]);
            if (level.isEmpty()) {
                return AdminCommandResult.failure("Level must be a positive whole number.");
            }
        }

        Optional<Player> target = args.length == 6 ? playerResolver.player(args[5]) : asPlayer(sender);
        if (target.isEmpty()) {
            return AdminCommandResult.failure("Specify an online player for the spawn target.");
        }

        return adminService.spawn(sender, target.get(), entityType.get(), args[2], args[3], level.get());
    }

    private boolean hasPermission(CommandSender sender, String subcommand) {
        return sender.hasPermission("mobrarity.%s".formatted(subcommand));
    }

    private static Optional<Player> asPlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return Optional.of(player);
        }
        return Optional.empty();
    }

    private static Optional<Integer> parseLevel(String value) {
        try {
            int level = Integer.parseInt(value);
            return level > 0 ? Optional.of(level) : Optional.empty();
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private static Optional<EntityType> parseEntityType(String value) {
        try {
            return Optional.of(EntityType.valueOf(value.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    @FunctionalInterface
    public interface AdminAction {
        AdminCommandResult run();
    }

    public interface AdminService {
        AdminCommandResult inspect(Player player);

        AdminCommandResult debug(Player player);

        AdminCommandResult set(Player player, String tierKey, String variantKey, int level);

        AdminCommandResult clear(Player player);

        AdminCommandResult spawn(
                CommandSender sender,
                Player target,
                EntityType entityType,
                String tierKey,
                String variantKey,
                int level);

        AdminCommandResult list(String category);

        Map<String, List<String>> completions();
    }

    @FunctionalInterface
    public interface PlayerResolver {
        Optional<Player> player(String name);
    }

    private static final class UnavailableAdminService implements AdminService {
        @Override
        public AdminCommandResult inspect(Player player) {
            return unavailable();
        }

        @Override
        public AdminCommandResult debug(Player player) {
            return unavailable();
        }

        @Override
        public AdminCommandResult set(Player player, String tierKey, String variantKey, int level) {
            return unavailable();
        }

        @Override
        public AdminCommandResult clear(Player player) {
            return unavailable();
        }

        @Override
        public AdminCommandResult spawn(
                CommandSender sender,
                Player target,
                EntityType entityType,
                String tierKey,
                String variantKey,
                int level) {
            return unavailable();
        }

        @Override
        public AdminCommandResult list(String category) {
            return unavailable();
        }

        @Override
        public Map<String, List<String>> completions() {
            return Map.of();
        }

        private static AdminCommandResult unavailable() {
            return AdminCommandResult.failure("MobRarity admin services are not available.");
        }
    }
}
