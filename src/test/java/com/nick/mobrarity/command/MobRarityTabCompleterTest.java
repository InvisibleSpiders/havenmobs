package com.nick.mobrarity.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

final class MobRarityTabCompleterTest {
    @Test
    void firstArgReturnsBaseSubcommands() {
        MobRarityTabCompleter completer = new MobRarityTabCompleter();

        assertThat(completer.onTabComplete(
                        mock(CommandSender.class),
                        mock(Command.class),
                        "mobrarity",
                        new String[] {""}))
                .containsExactly("reload", "inspect", "set", "spawn", "clear");
    }
}
