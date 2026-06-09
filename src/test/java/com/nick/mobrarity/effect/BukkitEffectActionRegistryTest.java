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
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
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
    void consoleCommandDispatchesConsoleCommandWithPlaceholders() {
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

        registry.action("console_command").orElseThrow().execute(
                new ActionDefinition("console_command", Map.of("command", "say %player% found %entity_type%")),
                TriggerContext.forEntity(entity, player));

        assertThat(dispatcher.consoleCommands).containsExactly("say Alex found SHEEP");
    }

    @Test
    void playerCommandDispatchesAsTriggeringPlayer() {
        Player player = mock(Player.class);
        RecordingCommandDispatcher dispatcher = new RecordingCommandDispatcher();
        BukkitEffectActionRegistry registry = registry(dispatcher);

        registry.action("player_command").orElseThrow().execute(
                new ActionDefinition("player_command", Map.of("command", "spawn")),
                TriggerContext.forEntity(mock(LivingEntity.class), player));

        assertThat(dispatcher.playerCommands).containsExactly("spawn");
        assertThat(dispatcher.player).isSameAs(player);
    }

    @Test
    void xpDropDropsExperienceAtEntityLocation() {
        LivingEntity entity = mock(LivingEntity.class);
        Location location = mock(Location.class);
        World world = mock(World.class);
        RecordingExperienceDropper experienceDropper = new RecordingExperienceDropper();
        when(entity.getLocation()).thenReturn(location);
        when(location.getWorld()).thenReturn(world);
        BukkitEffectActionRegistry registry = registry(experienceDropper);

        registry.action("xp_drop").orElseThrow().execute(
                new ActionDefinition("xp_drop", Map.of("amount", 7)),
                TriggerContext.forEntity(entity, null));

        assertThat(experienceDropper.world).isSameAs(world);
        assertThat(experienceDropper.location).isSameAs(location);
        assertThat(experienceDropper.amount).isEqualTo(7);
    }

    @Test
    void xpDropSupportsInclusiveAmountRangeUsingInjectedRandom() {
        LivingEntity entity = mock(LivingEntity.class);
        Location location = mock(Location.class);
        World world = mock(World.class);
        RecordingExperienceDropper experienceDropper = new RecordingExperienceDropper();
        when(entity.getLocation()).thenReturn(location);
        when(location.getWorld()).thenReturn(world);
        BukkitEffectActionRegistry registry = new BukkitEffectActionRegistry(
                new NoopEconomy(),
                new FixedRandom(2),
                new RecordingCommandDispatcher(),
                new RecordingItemDropper(),
                new RecordingPotionApplier(),
                experienceDropper,
                new RecordingHealthEditor(0, 20),
                new RecordingDamageDealer(),
                new RecordingVelocityApplier(),
                new RecordingWorldEffects());

        registry.action("xp_drop").orElseThrow().execute(
                new ActionDefinition("xp_drop", Map.of("amount", "2-8")),
                TriggerContext.forEntity(entity, null));

        assertThat(experienceDropper.amount).isEqualTo(4);
    }

    @Test
    void lootTableDropsConfiguredTableAtEntityLocation() {
        LivingEntity entity = mock(LivingEntity.class);
        Player player = mock(Player.class);
        Location location = mock(Location.class);
        World world = mock(World.class);
        RecordingLootTableDropper lootTableDropper = new RecordingLootTableDropper();
        when(entity.getLocation()).thenReturn(location);
        when(location.getWorld()).thenReturn(world);
        BukkitEffectActionRegistry registry = new BukkitEffectActionRegistry(
                new NoopEconomy(),
                new FixedRandom(0),
                new RecordingCommandDispatcher(),
                new RecordingItemDropper(),
                new RecordingPotionApplier(),
                new RecordingExperienceDropper(),
                new RecordingHealthEditor(0, 20),
                new RecordingDamageDealer(),
                new RecordingVelocityApplier(),
                new RecordingWorldEffects(),
                lootTableDropper);

        registry.action("loot_table").orElseThrow().execute(
                new ActionDefinition("loot_table", Map.of("table", "minecraft:entities/sheep", "rolls", 2)),
                TriggerContext.forEntity(entity, player));

        assertThat(lootTableDropper.location).isSameAs(location);
        assertThat(lootTableDropper.tableKey).isEqualTo(NamespacedKey.minecraft("entities/sheep"));
        assertThat(lootTableDropper.entity).isSameAs(entity);
        assertThat(lootTableDropper.player).isSameAs(player);
        assertThat(lootTableDropper.rolls).isEqualTo(2);
    }

    @Test
    void lootTableIgnoresInvalidTableKey() {
        LivingEntity entity = mock(LivingEntity.class);
        RecordingLootTableDropper lootTableDropper = new RecordingLootTableDropper();
        when(entity.getLocation()).thenReturn(mock(Location.class));
        BukkitEffectActionRegistry registry = new BukkitEffectActionRegistry(
                new NoopEconomy(),
                new FixedRandom(0),
                new RecordingCommandDispatcher(),
                new RecordingItemDropper(),
                new RecordingPotionApplier(),
                new RecordingExperienceDropper(),
                new RecordingHealthEditor(0, 20),
                new RecordingDamageDealer(),
                new RecordingVelocityApplier(),
                new RecordingWorldEffects(),
                lootTableDropper);

        registry.action("loot_table").orElseThrow().execute(
                new ActionDefinition("loot_table", Map.of("table", "not a key")),
                TriggerContext.forEntity(entity, mock(Player.class)));

        assertThat(lootTableDropper.tableKey).isNull();
    }

    @Test
    void healIncreasesTargetHealthUpToMaximum() {
        Player player = mock(Player.class);
        RecordingHealthEditor healthEditor = new RecordingHealthEditor(10.0, 14.0);
        BukkitEffectActionRegistry registry = registry(healthEditor);

        registry.action("heal").orElseThrow().execute(
                new ActionDefinition("heal", Map.of("amount", 8.0)),
                TriggerContext.forEntity(mock(LivingEntity.class), player));

        assertThat(healthEditor.target).isSameAs(player);
        assertThat(healthEditor.health).isEqualTo(14.0);
    }

    @Test
    void damageAppliesToConfiguredTarget() {
        LivingEntity entity = mock(LivingEntity.class);
        RecordingDamageDealer damageDealer = new RecordingDamageDealer();
        BukkitEffectActionRegistry registry = registry(damageDealer);

        registry.action("damage").orElseThrow().execute(
                new ActionDefinition("damage", Map.of("amount", 3.5, "target", "mob")),
                TriggerContext.forEntity(entity, mock(Player.class)));

        assertThat(damageDealer.target).isSameAs(entity);
        assertThat(damageDealer.source).isSameAs(entity);
        assertThat(damageDealer.amount).isEqualTo(3.5);
    }

    @Test
    void knockbackPushesPlayerAwayFromMob() {
        LivingEntity entity = mock(LivingEntity.class);
        Player player = mock(Player.class);
        Location entityLocation = location(0, 64, 0);
        Location playerLocation = location(3, 64, 4);
        RecordingVelocityApplier velocityApplier = new RecordingVelocityApplier();
        when(entity.getLocation()).thenReturn(entityLocation);
        when(player.getLocation()).thenReturn(playerLocation);
        BukkitEffectActionRegistry registry = registry(velocityApplier);

        registry.action("knockback").orElseThrow().execute(
                new ActionDefinition("knockback", Map.of("strength", 2.0, "y", 0.4)),
                TriggerContext.forEntity(entity, player));

        assertThat(velocityApplier.target).isSameAs(player);
        assertThat(velocityApplier.velocity.getX()).isCloseTo(1.2, org.assertj.core.data.Offset.offset(0.001));
        assertThat(velocityApplier.velocity.getY()).isEqualTo(0.4);
        assertThat(velocityApplier.velocity.getZ()).isCloseTo(1.6, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    void lightningEffectStrikesEntityLocationWithoutDamage() {
        LivingEntity entity = mock(LivingEntity.class);
        Location location = mock(Location.class);
        World world = mock(World.class);
        RecordingWorldEffects worldEffects = new RecordingWorldEffects();
        when(entity.getLocation()).thenReturn(location);
        when(location.getWorld()).thenReturn(world);
        BukkitEffectActionRegistry registry = registry(worldEffects);

        registry.action("lightning_effect").orElseThrow().execute(
                new ActionDefinition("lightning_effect", Map.of()),
                TriggerContext.forEntity(entity, mock(Player.class)));

        assertThat(worldEffects.world).isSameAs(world);
        assertThat(worldEffects.location).isSameAs(location);
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

    private static BukkitEffectActionRegistry registry(RecordingCommandDispatcher dispatcher) {
        return new BukkitEffectActionRegistry(
                new NoopEconomy(),
                new FixedRandom(0),
                dispatcher,
                new RecordingItemDropper(),
                new RecordingPotionApplier());
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

    private static BukkitEffectActionRegistry registry(RecordingExperienceDropper experienceDropper) {
        return registry(new RecordingItemDropper(), new RecordingPotionApplier(), experienceDropper);
    }

    private static BukkitEffectActionRegistry registry(RecordingHealthEditor healthEditor) {
        return registry(new RecordingItemDropper(), new RecordingPotionApplier(), healthEditor);
    }

    private static BukkitEffectActionRegistry registry(RecordingDamageDealer damageDealer) {
        return registry(new RecordingItemDropper(), new RecordingPotionApplier(), damageDealer);
    }

    private static BukkitEffectActionRegistry registry(RecordingVelocityApplier velocityApplier) {
        return registry(new RecordingItemDropper(), new RecordingPotionApplier(), velocityApplier);
    }

    private static BukkitEffectActionRegistry registry(RecordingWorldEffects worldEffects) {
        return registry(new RecordingItemDropper(), new RecordingPotionApplier(), worldEffects);
    }

    private static BukkitEffectActionRegistry registry(
            RecordingItemDropper dropper,
            RecordingPotionApplier potionApplier,
            Object extra) {
        return new BukkitEffectActionRegistry(
                new NoopEconomy(),
                new FixedRandom(0),
                new RecordingCommandDispatcher(),
                dropper,
                potionApplier,
                extra instanceof RecordingExperienceDropper experienceDropper
                        ? experienceDropper
                        : new RecordingExperienceDropper(),
                extra instanceof RecordingHealthEditor healthEditor ? healthEditor : new RecordingHealthEditor(0, 20),
                extra instanceof RecordingDamageDealer damageDealer ? damageDealer : new RecordingDamageDealer(),
                extra instanceof RecordingVelocityApplier velocityApplier ? velocityApplier : new RecordingVelocityApplier(),
                extra instanceof RecordingWorldEffects worldEffects ? worldEffects : new RecordingWorldEffects(),
                new RecordingLootTableDropper());
    }

    private static Location location(double x, double y, double z) {
        Location location = mock(Location.class);
        when(location.toVector()).thenReturn(new Vector(x, y, z));
        return location;
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
        private final List<String> playerCommands = new ArrayList<>();
        private Player player;

        @Override
        public void dispatchConsole(String command) {
            consoleCommands.add(command);
        }

        @Override
        public void dispatchPlayer(Player player, String command) {
            this.player = player;
            playerCommands.add(command);
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

    private static final class RecordingExperienceDropper implements BukkitEffectActionRegistry.ExperienceDropper {
        private World world;
        private Location location;
        private int amount;

        @Override
        public void drop(World world, Location location, int amount) {
            this.world = world;
            this.location = location;
            this.amount = amount;
        }
    }

    private static final class RecordingHealthEditor implements BukkitEffectActionRegistry.HealthEditor {
        private final double currentHealth;
        private final double maxHealth;
        private LivingEntity target;
        private double health;

        private RecordingHealthEditor(double currentHealth, double maxHealth) {
            this.currentHealth = currentHealth;
            this.maxHealth = maxHealth;
        }

        @Override
        public double health(LivingEntity target) {
            return currentHealth;
        }

        @Override
        public double maxHealth(LivingEntity target) {
            return maxHealth;
        }

        @Override
        public void setHealth(LivingEntity target, double health) {
            this.target = target;
            this.health = health;
        }
    }

    private static final class RecordingDamageDealer implements BukkitEffectActionRegistry.DamageDealer {
        private LivingEntity target;
        private LivingEntity source;
        private double amount;

        @Override
        public void damage(LivingEntity target, double amount, LivingEntity source) {
            this.target = target;
            this.amount = amount;
            this.source = source;
        }
    }

    private static final class RecordingVelocityApplier implements BukkitEffectActionRegistry.VelocityApplier {
        private LivingEntity target;
        private Vector velocity;

        @Override
        public void setVelocity(LivingEntity target, Vector velocity) {
            this.target = target;
            this.velocity = velocity;
        }
    }

    private static final class RecordingWorldEffects implements BukkitEffectActionRegistry.WorldEffects {
        private World world;
        private Location location;

        @Override
        public void strikeLightningEffect(World world, Location location) {
            this.world = world;
            this.location = location;
        }
    }

    private static final class RecordingLootTableDropper implements BukkitEffectActionRegistry.LootTableDropper {
        private Location location;
        private NamespacedKey tableKey;
        private LivingEntity entity;
        private Player player;
        private int rolls;

        @Override
        public void drop(Location location, NamespacedKey tableKey, LivingEntity entity, Player player, int rolls) {
            this.location = location;
            this.tableKey = tableKey;
            this.entity = entity;
            this.player = player;
            this.rolls = rolls;
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
