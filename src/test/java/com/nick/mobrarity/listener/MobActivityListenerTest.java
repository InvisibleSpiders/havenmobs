package com.nick.mobrarity.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nick.mobrarity.effect.ActionDefinition;
import com.nick.mobrarity.effect.EffectEngine;
import com.nick.mobrarity.effect.TriggerContext;
import com.nick.mobrarity.effect.TriggerDefinition;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.junit.jupiter.api.Test;

final class MobActivityListenerTest {
    @Test
    void shearHandlerIgnoresCancelledEvents() throws Exception {
        Method method = MobActivityListener.class.getDeclaredMethod("onPlayerShearEntity", PlayerShearEntityEvent.class);
        EventHandler handler = method.getAnnotation(EventHandler.class);

        assertThat(handler.ignoreCancelled()).isTrue();
    }

    @Test
    void runsShearTriggerWithPlayerAndEntityContext() {
        Player player = mock(Player.class);
        LivingEntity entity = mock(LivingEntity.class);
        PlayerShearEntityEvent event = mock(PlayerShearEntityEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.getEntity()).thenReturn(entity);
        TriggerDefinition trigger = new TriggerDefinition("on_shear", 1.0, 0, 0,
                List.of(new ActionDefinition("item_drop", Map.of())));
        List<TriggerContext> contexts = new ArrayList<>();
        EffectEngine effectEngine = new EffectEngine(type -> Optional.of((action, context) -> contexts.add(context)), () -> 0.0);
        MobActivityListener listener = new MobActivityListener(
                effectEngine,
                (target, triggerKey) -> "on_shear".equals(triggerKey) ? Optional.of(trigger) : Optional.empty());

        listener.onPlayerShearEntity(event);

        assertThat(contexts).hasSize(1);
        assertThat(contexts.getFirst().entity()).contains(entity);
        assertThat(contexts.getFirst().player()).contains(player);
    }

    @Test
    void skipsShearTriggerForNonLivingEntity() {
        PlayerShearEntityEvent event = mock(PlayerShearEntityEvent.class);
        when(event.getEntity()).thenReturn(mock(Entity.class));
        List<String> executed = new ArrayList<>();
        EffectEngine effectEngine = new EffectEngine(type -> Optional.of((action, context) -> executed.add(type)), () -> 0.0);
        MobActivityListener listener = new MobActivityListener(
                effectEngine,
                (target, triggerKey) -> Optional.of(new TriggerDefinition("on_shear", 1.0, 0, 0,
                        List.of(new ActionDefinition("item_drop", Map.of())))));

        listener.onPlayerShearEntity(event);

        assertThat(executed).isEmpty();
    }

    @Test
    void runsInteractTriggerWithPlayerAndEntityContext() {
        Player player = mock(Player.class);
        LivingEntity entity = mock(LivingEntity.class);
        PlayerInteractEntityEvent event = mock(PlayerInteractEntityEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.getRightClicked()).thenReturn(entity);
        List<TriggerContext> contexts = new ArrayList<>();
        MobActivityListener listener = listenerFor("on_interact", contexts);

        listener.onPlayerInteractEntity(event);

        assertThat(contexts).hasSize(1);
        assertThat(contexts.getFirst().entity()).contains(entity);
        assertThat(contexts.getFirst().player()).contains(player);
    }

    @Test
    void runsTameTriggerWhenOwnerIsPlayer() {
        Player player = mock(Player.class);
        LivingEntity entity = mock(LivingEntity.class);
        EntityTameEvent event = mock(EntityTameEvent.class);
        when(event.getOwner()).thenReturn(player);
        when(event.getEntity()).thenReturn(entity);
        List<TriggerContext> contexts = new ArrayList<>();
        MobActivityListener listener = listenerFor("on_tame", contexts);

        listener.onEntityTame(event);

        assertThat(contexts).hasSize(1);
        assertThat(contexts.getFirst().entity()).contains(entity);
        assertThat(contexts.getFirst().player()).contains(player);
    }

    @Test
    void runsBreedTriggerWhenBreederIsPlayer() {
        Player player = mock(Player.class);
        LivingEntity child = mock(LivingEntity.class);
        EntityBreedEvent event = mock(EntityBreedEvent.class);
        when(event.getBreeder()).thenReturn(player);
        when(event.getEntity()).thenReturn(child);
        List<TriggerContext> contexts = new ArrayList<>();
        MobActivityListener listener = listenerFor("on_breed", contexts);

        listener.onEntityBreed(event);

        assertThat(contexts).hasSize(1);
        assertThat(contexts.getFirst().entity()).contains(child);
        assertThat(contexts.getFirst().player()).contains(player);
    }

    private static MobActivityListener listenerFor(String expectedTrigger, List<TriggerContext> contexts) {
        EffectEngine effectEngine = new EffectEngine(
                type -> Optional.of((action, context) -> contexts.add(context)),
                () -> 0.0);
        return new MobActivityListener(
                effectEngine,
                (target, triggerKey) -> expectedTrigger.equals(triggerKey)
                        ? Optional.of(new TriggerDefinition(expectedTrigger, 1.0, 0, 0,
                                List.of(new ActionDefinition("command", Map.of()))))
                        : Optional.empty());
    }
}
