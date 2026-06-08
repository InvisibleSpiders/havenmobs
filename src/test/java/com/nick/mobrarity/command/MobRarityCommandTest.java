package com.nick.mobrarity.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

final class MobRarityCommandTest {
    @Test
    void noArgsSendsUsageAndReturnsTrue() {
        MobRarityCommand command = new MobRarityCommand();
        CommandSender sender = mock(CommandSender.class);

        boolean handled = command.onCommand(sender, mock(Command.class), "mobrarity", new String[0]);

        assertThat(handled).isTrue();
        verify(sender).sendMessage("/mobrarity reload|inspect|set|spawn|clear");
    }
}
