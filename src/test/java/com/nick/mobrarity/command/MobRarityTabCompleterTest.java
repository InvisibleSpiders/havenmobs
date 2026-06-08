package com.nick.mobrarity.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Map;
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

    @Test
    void setSuggestsConfiguredTiersAndVariants() {
        MobRarityTabCompleter completer = new MobRarityTabCompleter(
                () -> Map.of("tiers", List.of("common", "rare"), "variants", List.of("rare_sheep")),
                () -> List.of());

        assertThat(completer.onTabComplete(
                        mock(CommandSender.class),
                        mock(Command.class),
                        "mobrarity",
                        new String[] {"set", "r"}))
                .containsExactly("rare");
        assertThat(completer.onTabComplete(
                        mock(CommandSender.class),
                        mock(Command.class),
                        "mobrarity",
                        new String[] {"set", "rare", ""}))
                .containsExactly("rare_sheep");
    }

    @Test
    void spawnSuggestsEntityTypesTiersVariantsAndPlayers() {
        MobRarityTabCompleter completer = new MobRarityTabCompleter(
                () -> Map.of("tiers", List.of("rare"), "variants", List.of("rare_sheep")),
                () -> List.of("Alex"));

        assertThat(completer.onTabComplete(
                        mock(CommandSender.class),
                        mock(Command.class),
                        "mobrarity",
                        new String[] {"spawn", "she"}))
                .contains("SHEEP");
        assertThat(completer.onTabComplete(
                        mock(CommandSender.class),
                        mock(Command.class),
                        "mobrarity",
                        new String[] {"spawn", "SHEEP", ""}))
                .containsExactly("rare");
        assertThat(completer.onTabComplete(
                        mock(CommandSender.class),
                        mock(Command.class),
                        "mobrarity",
                        new String[] {"spawn", "SHEEP", "rare", ""}))
                .containsExactly("rare_sheep");
        assertThat(completer.onTabComplete(
                        mock(CommandSender.class),
                        mock(Command.class),
                        "mobrarity",
                        new String[] {"spawn", "SHEEP", "rare", "rare_sheep", "1", "A"}))
                .containsExactly("Alex");
    }
}
