package com.nick.mobrarity.effect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nick.mobrarity.integration.EconomyAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

final class BukkitEffectActionRegistryTest {
    @Test
    void itemDropDropsConfiguredMaterialAndAmountAtEntityLocation() {
        LivingEntity entity = mock(LivingEntity.class);
        Location location = mock(Location.class);
        World world = mock(World.class);
        RecordingItemDropper dropper = new RecordingItemDropper();
        when(entity.getLocation()).thenReturn(location);
        when(location.getWorld()).thenReturn(world);
        BukkitEffectActionRegistry registry = registry(dropper);

        registry.action("item_drop").orElseThrow().execute(
                new ActionDefinition("item_drop", Map.of("material", "DIAMOND", "amount", 2)),
                TriggerContext.forEntity(entity, null));

        assertThat(dropper.world).isSameAs(world);
        assertThat(dropper.location).isSameAs(location);
        assertThat(dropper.material).isEqualTo(Material.DIAMOND);
        assertThat(dropper.amount).isEqualTo(2);
    }

    @Test
    void itemDropSupportsInclusiveAmountRange() {
        LivingEntity entity = mock(LivingEntity.class);
        Location location = mock(Location.class);
        World world = mock(World.class);
        RecordingItemDropper dropper = new RecordingItemDropper();
        when(entity.getLocation()).thenReturn(location);
        when(location.getWorld()).thenReturn(world);
        BukkitEffectActionRegistry registry = new BukkitEffectActionRegistry(
                new NoopEconomy(),
                new FixedRandom(2),
                new RecordingCommandDispatcher(),
                dropper,
                new RecordingPotionApplier());

        registry.action("item_drop").orElseThrow().execute(
                new ActionDefinition("item_drop", Map.of("material", "GOLD_NUGGET", "amount", "2-8")),
                TriggerContext.forEntity(entity, null));

        assertThat(dropper.material).isEqualTo(Material.GOLD_NUGGET);
        assertThat(dropper.amount).isEqualTo(4);
    }

    @Test
    void potionEffectAppliesToPlayerByDefault() {
        Player player = mock(Player.class);
        RecordingPotionApplier potionApplier = new RecordingPotionApplier();
        BukkitEffectActionRegistry registry = registry(new RecordingItemDropper(), potionApplier);

        registry.action("potion_effect").orElseThrow().execute(
                new ActionDefinition("potion_effect", Map.of(
                        "effect", "POISON",
                        "duration-ticks", 80,
                        "amplifier", 1)),
                TriggerContext.forEntity(mock(LivingEntity.class), player));

        assertThat(potionApplier.target).isSameAs(player);
        assertThat(potionApplier.effectName).isEqualTo("POISON");
        assertThat(potionApplier.durationTicks).isEqualTo(80);
        assertThat(potionApplier.amplifier).isEqualTo(1);
    }

    @Test
    void currencyDropDepositsToPlayer() {
        Player player = mock(Player.class);
        RecordingEconomy economy = new RecordingEconomy();
        BukkitEffectActionRegistry registry = new BukkitEffectActionRegistry(
                economy,
                new FixedRandom(0),
                new RecordingCommandDispatcher(),
                new RecordingItemDropper(),
                new RecordingPotionApplier());

        registry.action("currency_drop").orElseThrow().execute(
                new ActionDefinition("currency_drop", Map.of("amount", 12.5)),
                TriggerContext.forEntity(mock(LivingEntity.class), player));

        assertThat(economy.player).isSameAs(player);
        assertThat(economy.amount).isEqualTo(12.5);
    }

    @Test
    void commandActionDispatchesConsoleCommandWithPlaceholders() {
        Player player = mock(Player.class);
        when(player.getName()).thenReturn("Alex");
        LivingEntity entity = mock(LivingEntity.class);
        when(entity.getType()).thenReturn(org.bukkit.entity.EntityType.SHEEP);
        RecordingCommandDispatcher dispatcher = new RecordingCommandDispatcher();
        BukkitEffectActionRegistry registry = new BukkitEffectActionRegistry(
                new NoopEconomy(),
                new FixedRandom(0),
                dispatcher,
                new RecordingItemDropper(),
                new RecordingPotionApplier());

        registry.action("command").orElseThrow().execute(
                new ActionDefinition("command", Map.of("command", "say %player% found %entity_type%")),
                TriggerContext.forEntity(entity, player));

        assertThat(dispatcher.consoleCommands).containsExactly("say Alex found SHEEP");
    }

    @Test
    void hostileTargetSetsMobTargetToPlayer() {
        Player player = mock(Player.class);
        Mob mob = mock(Mob.class);
        BukkitEffectActionRegistry registry = registry();

        registry.action("hostile_target").orElseThrow().execute(
                new ActionDefinition("hostile_target", Map.of()),
                TriggerContext.forEntity(mob, player));

        org.mockito.Mockito.verify(mob).setTarget(player);
    }

    @Test
    void hostileTargetIgnoresNonMobEntities() {
        Player player = mock(Player.class);
        LivingEntity entity = mock(LivingEntity.class);
        BukkitEffectActionRegistry registry = registry();

        registry.action("hostile_target").orElseThrow().execute(
                new ActionDefinition("hostile_target", Map.of()),
                TriggerContext.forEntity(entity, player));

        assertThat(registry.action("hostile_target")).isPresent();
    }

    private static BukkitEffectActionRegistry registry() {
        return registry(new RecordingItemDropper(), new RecordingPotionApplier());
    }

    private static BukkitEffectActionRegistry registry(RecordingItemDropper dropper) {
        return registry(dropper, new RecordingPotionApplier());
    }

    private static BukkitEffectActionRegistry registry(
            RecordingItemDropper dropper,
            RecordingPotionApplier potionApplier) {
        return new BukkitEffectActionRegistry(
                new NoopEconomy(),
                new FixedRandom(0),
                new RecordingCommandDispatcher(),
                dropper,
                potionApplier);
    }

    private static final class RecordingEconomy implements EconomyAdapter {
        private Player player;
        private double amount;

        @Override
        public boolean available() {
            return true;
        }

        @Override
        public void deposit(Player player, double amount) {
            this.player = player;
            this.amount = amount;
        }
    }

    private static final class NoopEconomy implements EconomyAdapter {
        @Override
        public boolean available() {
            return false;
        }

        @Override
        public void deposit(Player player, double amount) {
        }
    }

    private static final class RecordingCommandDispatcher implements BukkitEffectActionRegistry.CommandDispatcher {
        private final List<String> consoleCommands = new ArrayList<>();

        @Override
        public void dispatchConsole(String command) {
            consoleCommands.add(command);
        }

        @Override
        public void dispatchPlayer(Player player, String command) {
        }
    }

    private static final class RecordingItemDropper implements BukkitEffectActionRegistry.ItemDropper {
        private World world;
        private Location location;
        private Material material;
        private int amount;

        @Override
        public void drop(World world, Location location, Material material, int amount) {
            this.world = world;
            this.location = location;
            this.material = material;
            this.amount = amount;
        }
    }

    private static final class RecordingPotionApplier implements BukkitEffectActionRegistry.PotionApplier {
        private LivingEntity target;
        private String effectName;
        private int durationTicks;
        private int amplifier;

        @Override
        public void apply(LivingEntity target, String effectName, int durationTicks, int amplifier) {
            this.target = target;
            this.effectName = effectName;
            this.durationTicks = durationTicks;
            this.amplifier = amplifier;
        }
    }

    private static final class FixedRandom implements RandomGenerator {
        private final int value;

        private FixedRandom(int value) {
            this.value = value;
        }

        @Override
        public long nextLong() {
            return value;
        }

        @Override
        public int nextInt(int bound) {
            return value;
        }
    }
}
