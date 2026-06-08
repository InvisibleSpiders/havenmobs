package com.nick.mobrarity.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public final class MobRarityCommand implements CommandExecutor {
    static final String USAGE = "/mobrarity reload|inspect|set|spawn|clear";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args == null || args.length == 0) {
            if (sender != null) {
                sender.sendMessage(USAGE);
            }
            return true;
        }

        return true;
    }
}
