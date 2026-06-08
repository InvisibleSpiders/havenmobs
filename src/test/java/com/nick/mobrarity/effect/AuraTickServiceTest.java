package com.nick.mobrarity.effect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nick.mobrarity.integration.ProtectionAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

final class AuraTickServiceTest {
    @Test
    void runsAuraTriggerForNearbyPlayerWhenIntervalHasElapsed() {
        World world = mock(World.class);
        LivingEntity mob = mob(world, 0, 64, 0);
        Player player = player(world, 2, 64, 0);
        TriggerDefinition trigger = trigger(40, 4.0);
        List<TriggerContext> contexts = new ArrayList<>();
        AuraTickService service = service(
                List.of(mob),
                List.of(player),
                (entity, triggerKey) -> Optional.of(trigger),
                allowAll(),
                contexts);

        service.tick(40);

        assertThat(contexts).hasSize(1);
        assertThat(contexts.getFirst().entity()).contains(mob);
        assertThat(contexts.getFirst().player()).contains(player);
    }

    @Test
    void skipsAuraTriggerUntilIntervalElapses() {
        World world = mock(World.class);
        LivingEntity mob = mob(world, 0, 64, 0);
        Player player = player(world, 2, 64, 0);
        List<TriggerContext> contexts = new ArrayList<>();
        AuraTickService service = service(
                List.of(mob),
                List.of(player),
                (entity, triggerKey) -> Optional.of(trigger(40, 4.0)),
                allowAll(),
                contexts);

        service.tick(39);

        assertThat(contexts).isEmpty();
    }

    @Test
    void skipsPlayersOutsideRadiusOrInOtherWorlds() {
        World world = mock(World.class);
        World otherWorld = mock(World.class);
        LivingEntity mob = mob(world, 0, 64, 0);
        Player far = player(world, 10, 64, 0);
        Player other = player(otherWorld, 0, 64, 0);
        List<TriggerContext> contexts = new ArrayList<>();
        AuraTickService service = service(
                List.of(mob),
                List.of(far, other),
                (entity, triggerKey) -> Optional.of(trigger(20, 4.0)),
                allowAll(),
                contexts);

        service.tick(20);

        assertThat(contexts).isEmpty();
    }

    @Test
    void protectionCanBlockAuraActionsForPlayer() {
        World world = mock(World.class);
        LivingEntity mob = mob(world, 0, 64, 0);
        Player player = player(world, 2, 64, 0);
        List<TriggerContext> contexts = new ArrayList<>();
        AuraTickService service = service(
                List.of(mob),
                List.of(player),
                (entity, triggerKey) -> Optional.of(trigger(20, 4.0)),
                (target, location, actionKey, actionType) -> false,
                contexts);

        service.tick(20);

        assertThat(contexts).isEmpty();
    }

    @Test
    void deadOrInvalidMobsAreIgnored() {
        World world = mock(World.class);
        LivingEntity mob = mob(world, 0, 64, 0);
        when(mob.isValid()).thenReturn(false);
        Player player = player(world, 2, 64, 0);
        List<TriggerContext> contexts = new ArrayList<>();
        AuraTickService service = service(
                List.of(mob),
                List.of(player),
                (entity, triggerKey) -> Optional.of(trigger(20, 4.0)),
                allowAll(),
                contexts);

        service.tick(20);

        assertThat(contexts).isEmpty();
    }

    private static AuraTickService service(
            List<LivingEntity> mobs,
            List<Player> players,
            AuraTickService.TriggerLookup triggerLookup,
            ProtectionAdapter protectionAdapter,
            List<TriggerContext> contexts) {
        EffectEngine engine = new EffectEngine(type -> Optional.of((action, context) -> contexts.add(context)), () -> 0.0);
        return new AuraTickService(
                () -> mobs,
                () -> players,
                triggerLookup,
                protectionAdapter,
                engine);
    }

    private static ProtectionAdapter allowAll() {
        return (player, location, actionKey, actionType) -> true;
    }

    private static TriggerDefinition trigger(long intervalTicks, double radius) {
        return new TriggerDefinition("on_aura_tick", 1.0, intervalTicks, radius,
                List.of(new ActionDefinition("potion_effect", Map.of("effect", "POISON"))));
    }

    private static LivingEntity mob(World world, double x, double y, double z) {
        LivingEntity entity = mock(LivingEntity.class);
        when(entity.getUniqueId()).thenReturn(UUID.randomUUID());
        when(entity.getWorld()).thenReturn(world);
        when(entity.getLocation()).thenReturn(new Location(world, x, y, z));
        when(entity.isValid()).thenReturn(true);
        when(entity.isDead()).thenReturn(false);
        return entity;
    }

    private static Player player(World world, double x, double y, double z) {
        Player player = mock(Player.class);
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(new Location(world, x, y, z));
        return player;
    }
}
