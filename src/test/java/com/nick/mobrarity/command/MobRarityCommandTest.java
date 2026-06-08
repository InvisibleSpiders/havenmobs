package com.nick.mobrarity.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

final class MobRarityCommandTest {
    @Test
    void noArgsSendsUsageAndReturnsTrue() {
        MobRarityCommand command = new MobRarityCommand();
        CommandSender sender = mock(CommandSender.class);

        boolean handled = command.onCommand(sender, mock(Command.class), "mobrarity", new String[0]);

        assertThat(handled).isTrue();
        verify(sender).sendMessage("/mobrarity reload|validate|list|inspect|set|spawn|clear");
    }

    @Test
    void reloadRequiresPermissionBeforeReloading() {
        MobRarityCommand.AdminAction reload = mock(MobRarityCommand.AdminAction.class);
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("mobrarity.reload")).thenReturn(false);
        MobRarityCommand command = new MobRarityCommand(
                StubAdminService.empty(),
                reload,
                () -> AdminCommandResult.success("MobRarity config is valid."),
                name -> Optional.empty(),
                () -> List.of());

        boolean handled = command.onCommand(sender, mock(Command.class), "mobrarity", new String[] {"reload"});

        assertThat(handled).isTrue();
        verify(sender).sendMessage("You do not have permission to use /mobrarity reload.");
        verify(reload, never()).run();
    }

    @Test
    void inspectRequiresPlayerSenderAndReportsTargetData() {
        Player player = permittedPlayer("mobrarity.inspect");
        StubAdminService adminService = StubAdminService.empty();
        adminService.inspectResult = AdminCommandResult.success("Target: SHEEP rare/rare_sheep level 7");
        MobRarityCommand command = new MobRarityCommand(
                adminService,
                () -> AdminCommandResult.success("Reloaded MobRarity config."),
                () -> AdminCommandResult.success("MobRarity config is valid."),
                name -> Optional.empty(),
                () -> List.of());

        boolean handled = command.onCommand(player, mock(Command.class), "mobrarity", new String[] {"inspect"});

        assertThat(handled).isTrue();
        assertThat(adminService.inspectPlayer).isSameAs(player);
        verify(player).sendMessage("Target: SHEEP rare/rare_sheep level 7");
    }

    @Test
    void setParsesTierVariantAndLevelForTargetedMob() {
        Player player = permittedPlayer("mobrarity.set");
        StubAdminService adminService = StubAdminService.empty();
        adminService.setResult = AdminCommandResult.success("Set target to rare/rare_sheep level 5.");
        MobRarityCommand command = new MobRarityCommand(
                adminService,
                () -> AdminCommandResult.success("Reloaded MobRarity config."),
                () -> AdminCommandResult.success("MobRarity config is valid."),
                name -> Optional.empty(),
                () -> List.of());

        boolean handled = command.onCommand(
                player,
                mock(Command.class),
                "mobrarity",
                new String[] {"set", "rare", "rare_sheep", "5"});

        assertThat(handled).isTrue();
        assertThat(adminService.setPlayer).isSameAs(player);
        assertThat(adminService.setTier).isEqualTo("rare");
        assertThat(adminService.setVariant).isEqualTo("rare_sheep");
        assertThat(adminService.setLevel).isEqualTo(5);
        verify(player).sendMessage("Set target to rare/rare_sheep level 5.");
    }

    @Test
    void spawnCanTargetNamedPlayer() {
        Player sender = permittedPlayer("mobrarity.spawn");
        Player target = mock(Player.class);
        StubAdminService adminService = StubAdminService.empty();
        adminService.spawnResult = AdminCommandResult.success("Spawned SHEEP rare/rare_sheep level 3 on Alex.");
        MobRarityCommand command = new MobRarityCommand(
                adminService,
                () -> AdminCommandResult.success("Reloaded MobRarity config."),
                () -> AdminCommandResult.success("MobRarity config is valid."),
                name -> "Alex".equals(name) ? Optional.of(target) : Optional.empty(),
                () -> List.of("Alex"));

        boolean handled = command.onCommand(
                sender,
                mock(Command.class),
                "mobrarity",
                new String[] {"spawn", "SHEEP", "rare", "rare_sheep", "3", "Alex"});

        assertThat(handled).isTrue();
        assertThat(adminService.spawnSender).isSameAs(sender);
        assertThat(adminService.spawnTarget).isSameAs(target);
        assertThat(adminService.spawnEntityType).isEqualTo(EntityType.SHEEP);
        assertThat(adminService.spawnTier).isEqualTo("rare");
        assertThat(adminService.spawnVariant).isEqualTo("rare_sheep");
        assertThat(adminService.spawnLevel).isEqualTo(3);
        verify(sender).sendMessage("Spawned SHEEP rare/rare_sheep level 3 on Alex.");
    }

    @Test
    void clearRoutesToAdminService() {
        Player player = permittedPlayer("mobrarity.clear");
        StubAdminService adminService = StubAdminService.empty();
        adminService.clearResult = AdminCommandResult.success("Cleared MobRarity data from target.");
        MobRarityCommand command = new MobRarityCommand(
                adminService,
                () -> AdminCommandResult.success("Reloaded MobRarity config."),
                () -> AdminCommandResult.success("MobRarity config is valid."),
                name -> Optional.empty(),
                () -> List.of());

        boolean handled = command.onCommand(player, mock(Command.class), "mobrarity", new String[] {"clear"});

        assertThat(handled).isTrue();
        assertThat(adminService.clearPlayer).isSameAs(player);
        verify(player).sendMessage("Cleared MobRarity data from target.");
    }

    @Test
    void invalidLevelSendsUsageBeforeCallingSet() {
        Player player = permittedPlayer("mobrarity.set");
        StubAdminService adminService = StubAdminService.empty();
        MobRarityCommand command = new MobRarityCommand(
                adminService,
                () -> AdminCommandResult.success("Reloaded MobRarity config."),
                () -> AdminCommandResult.success("MobRarity config is valid."),
                name -> Optional.empty(),
                () -> List.of());

        boolean handled = command.onCommand(
                player,
                mock(Command.class),
                "mobrarity",
                new String[] {"set", "rare", "rare_sheep", "nope"});

        assertThat(handled).isTrue();
        assertThat(adminService.setPlayer).isNull();
        verify(player).sendMessage("Level must be a positive whole number.");
    }

    @Test
    void validateRequiresPermissionAndRunsValidationAction() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("mobrarity.validate")).thenReturn(true);
        MobRarityCommand.AdminAction validate = mock(MobRarityCommand.AdminAction.class);
        when(validate.run()).thenReturn(AdminCommandResult.success("MobRarity config is valid."));
        MobRarityCommand command = new MobRarityCommand(
                StubAdminService.empty(),
                () -> AdminCommandResult.success("Reloaded MobRarity config."),
                validate,
                name -> Optional.empty(),
                () -> List.of());

        boolean handled = command.onCommand(sender, mock(Command.class), "mobrarity", new String[] {"validate"});

        assertThat(handled).isTrue();
        verify(validate).run();
        verify(sender).sendMessage("MobRarity config is valid.");
    }

    @Test
    void listRoutesCategoryToAdminService() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("mobrarity.list")).thenReturn(true);
        StubAdminService adminService = StubAdminService.empty();
        adminService.listResult = AdminCommandResult.success("Tiers: common, rare");
        MobRarityCommand command = new MobRarityCommand(
                adminService,
                () -> AdminCommandResult.success("Reloaded MobRarity config."),
                () -> AdminCommandResult.success("MobRarity config is valid."),
                name -> Optional.empty(),
                () -> List.of());

        boolean handled = command.onCommand(sender, mock(Command.class), "mobrarity", new String[] {"list", "tiers"});

        assertThat(handled).isTrue();
        assertThat(adminService.listCategory).isEqualTo("tiers");
        verify(sender).sendMessage("Tiers: common, rare");
    }

    private static Player permittedPlayer(String permission) {
        Player player = mock(Player.class);
        when(player.hasPermission(permission)).thenReturn(true);
        return player;
    }

    private static final class StubAdminService implements MobRarityCommand.AdminService {
        private AdminCommandResult inspectResult = AdminCommandResult.failure("No targeted living mob found.");
        private AdminCommandResult setResult = AdminCommandResult.failure("No targeted living mob found.");
        private AdminCommandResult clearResult = AdminCommandResult.failure("No targeted living mob found.");
        private AdminCommandResult spawnResult = AdminCommandResult.failure("Unable to spawn entity.");
        private AdminCommandResult listResult = AdminCommandResult.failure("Unknown list category.");
        private Player inspectPlayer;
        private Player setPlayer;
        private String setTier;
        private String setVariant;
        private int setLevel;
        private Player clearPlayer;
        private String listCategory;
        private CommandSender spawnSender;
        private Player spawnTarget;
        private EntityType spawnEntityType;
        private String spawnTier;
        private String spawnVariant;
        private int spawnLevel;

        static StubAdminService empty() {
            return new StubAdminService();
        }

        @Override
        public AdminCommandResult inspect(Player player) {
            inspectPlayer = player;
            return inspectResult;
        }

        @Override
        public AdminCommandResult set(Player player, String tierKey, String variantKey, int level) {
            setPlayer = player;
            setTier = tierKey;
            setVariant = variantKey;
            setLevel = level;
            return setResult;
        }

        @Override
        public AdminCommandResult clear(Player player) {
            clearPlayer = player;
            return clearResult;
        }

        @Override
        public AdminCommandResult spawn(
                CommandSender sender,
                Player target,
                EntityType entityType,
                String tierKey,
                String variantKey,
                int level) {
            spawnSender = sender;
            spawnTarget = target;
            spawnEntityType = entityType;
            spawnTier = tierKey;
            spawnVariant = variantKey;
            spawnLevel = level;
            return spawnResult;
        }

        @Override
        public AdminCommandResult list(String category) {
            listCategory = category;
            return listResult;
        }

        @Override
        public Map<String, List<String>> completions() {
            return Map.of();
        }
    }
}
