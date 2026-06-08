package com.nick.mobrarity.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nick.mobrarity.effect.ActionDefinition;
import com.nick.mobrarity.effect.EffectEngine;
import com.nick.mobrarity.effect.TriggerDefinition;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.junit.jupiter.api.Test;

final class MobDeathListenerTest {
    @Test
    void entityDeathHandlerIgnoresCancelledEventsWhenSupported() throws Exception {
        Method method = MobDeathListener.class.getDeclaredMethod("onEntityDeath", EntityDeathEvent.class);
        EventHandler handler = method.getAnnotation(EventHandler.class);

        assertThat(handler.ignoreCancelled()).isTrue();
    }

    @Test
    void runsDeathTriggerForPlayerCausedFatalDamageWithRecentPlayerDamage() {
        UUID entityId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        LivingEntity entity = mockLivingEntity(entityId);
        Player player = mockPlayer(playerId);
        EntityDamageByEntityEvent fatalCause = mock(EntityDamageByEntityEvent.class);
        when(fatalCause.getDamager()).thenReturn(player);
        EntityDeathEvent deathEvent = mock(EntityDeathEvent.class);
        when(deathEvent.getEntity()).thenReturn(entity);
        when(entity.getLastDamageCause()).thenReturn(fatalCause);
        PlayerDamageTracker tracker = new PlayerDamageTracker(100);
        tracker.record(entityId, playerId, 40);
        List<String> executed = new ArrayList<>();
        EffectEngine effectEngine = new EffectEngine(type -> Optional.of(context -> executed.add(type)), () -> 0.0);
        TriggerDefinition trigger = new TriggerDefinition("on_death", 1.0, 0, 0,
                List.of(new ActionDefinition("message", Map.of())));
        MobDeathListener listener = new MobDeathListener(tracker, effectEngine, ignored -> Optional.of(trigger), () -> 100);

        listener.onEntityDeath(deathEvent);

        assertThat(executed).containsExactly("message");
    }

    @Test
    void skipsDeathTriggerWhenRecentPlayerDamageIsFollowedByNaturalFatalDamage() {
        UUID entityId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        LivingEntity entity = mockLivingEntity(entityId);
        EntityDamageEvent fatalCause = mock(EntityDamageEvent.class);
        EntityDeathEvent deathEvent = mock(EntityDeathEvent.class);
        when(deathEvent.getEntity()).thenReturn(entity);
        when(entity.getLastDamageCause()).thenReturn(fatalCause);
        PlayerDamageTracker tracker = new PlayerDamageTracker(100);
        tracker.record(entityId, playerId, 40);
        List<String> executed = new ArrayList<>();
        EffectEngine effectEngine = new EffectEngine(type -> Optional.of(context -> executed.add(type)), () -> 0.0);
        TriggerDefinition trigger = new TriggerDefinition("on_death", 1.0, 0, 0,
                List.of(new ActionDefinition("message", Map.of())));
        MobDeathListener listener = new MobDeathListener(tracker, effectEngine, ignored -> Optional.of(trigger), () -> 100);

        listener.onEntityDeath(deathEvent);

        assertThat(executed).isEmpty();
    }

    @Test
    void skipsDeathTriggerWithoutRecentPlayerDamage() {
        LivingEntity entity = mockLivingEntity(UUID.randomUUID());
        Player player = mockPlayer(UUID.randomUUID());
        EntityDamageByEntityEvent fatalCause = mock(EntityDamageByEntityEvent.class);
        EntityDeathEvent deathEvent = mock(EntityDeathEvent.class);
        when(fatalCause.getDamager()).thenReturn(player);
        when(deathEvent.getEntity()).thenReturn(entity);
        when(entity.getLastDamageCause()).thenReturn(fatalCause);
        List<String> executed = new ArrayList<>();
        EffectEngine effectEngine = new EffectEngine(type -> Optional.of(context -> executed.add(type)), () -> 0.0);
        TriggerDefinition trigger = new TriggerDefinition("on_death", 1.0, 0, 0,
                List.of(new ActionDefinition("message", Map.of())));
        MobDeathListener listener = new MobDeathListener(
                new PlayerDamageTracker(100),
                effectEngine,
                ignored -> Optional.of(trigger),
                () -> 100);

        listener.onEntityDeath(deathEvent);

        assertThat(executed).isEmpty();
    }

    private static LivingEntity mockLivingEntity(UUID uniqueId) {
        LivingEntity entity = mock(LivingEntity.class);
        when(entity.getUniqueId()).thenReturn(uniqueId);
        return entity;
    }

    private static Player mockPlayer(UUID uniqueId) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uniqueId);
        return player;
    }
}
