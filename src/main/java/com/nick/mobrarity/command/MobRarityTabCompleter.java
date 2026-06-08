package com.nick.mobrarity.command;

import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public final class MobRarityTabCompleter implements TabCompleter {
    private static final List<String> BASE_SUBCOMMANDS = List.of("reload", "inspect", "set", "spawn", "clear");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args == null || args.length <= 1) {
            String prefix = args == null || args.length == 0 || args[0] == null
                    ? ""
                    : args[0].toLowerCase(Locale.ROOT);
            return BASE_SUBCOMMANDS.stream()
                    .filter(subcommand -> subcommand.startsWith(prefix))
                    .toList();
        }

        return List.of();
    }
}
