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
import java.util.UUID;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.junit.jupiter.api.Test;

final class MobDamageListenerTest {
    @Test
    void entityDamageHandlerIgnoresCancelledEvents() throws Exception {
        Method method = MobDamageListener.class.getDeclaredMethod("onEntityDamageByEntity", EntityDamageByEntityEvent.class);
        EventHandler handler = method.getAnnotation(EventHandler.class);

        assertThat(handler.ignoreCancelled()).isTrue();
    }

    @Test
    void recordsDirectPlayerDamage() {
        PlayerDamageTracker tracker = new PlayerDamageTracker(100);
        MobDamageListener listener = new MobDamageListener(tracker, () -> 75);
        LivingEntity damaged = mockLivingEntity(UUID.randomUUID());
        Player player = mockPlayer(UUID.randomUUID());

        listener.recordDamage(damaged, player);

        assertThat(tracker.lastPlayer(damaged.getUniqueId(), 75)).contains(player.getUniqueId());
    }

    @Test
    void runsDamageTriggerForPlayerDamageWithContext() {
        PlayerDamageTracker tracker = new PlayerDamageTracker(100);
        LivingEntity damaged = mockLivingEntity(UUID.randomUUID());
        Player player = mockPlayer(UUID.randomUUID());
        TriggerDefinition trigger = trigger("on_damage");
        List<TriggerContext> contexts = new ArrayList<>();
        EffectEngine effectEngine = new EffectEngine(
                type -> Optional.of((action, context) -> contexts.add(context)),
                () -> 0.0);
        MobDamageListener listener = new MobDamageListener(
                tracker,
                () -> 75,
                effectEngine,
                (entity, triggerKey) -> "on_damage".equals(triggerKey) ? Optional.of(trigger) : Optional.empty());

        listener.recordDamage(damaged, player);

        assertThat(tracker.lastPlayer(damaged.getUniqueId(), 75)).contains(player.getUniqueId());
        assertThat(contexts).hasSize(1);
        assertThat(contexts.getFirst().entity()).contains(damaged);
        assertThat(contexts.getFirst().player()).contains(player);
    }

    @Test
    void skipsDamageTriggerForNonPlayerDamage() {
        PlayerDamageTracker tracker = new PlayerDamageTracker(100);
        LivingEntity damaged = mockLivingEntity(UUID.randomUUID());
        Entity damager = mock(Entity.class);
        List<String> executed = new ArrayList<>();
        EffectEngine effectEngine = new EffectEngine(
                type -> Optional.of((action, context) -> executed.add(type)),
                () -> 0.0);
        MobDamageListener listener = new MobDamageListener(
                tracker,
                () -> 75,
                effectEngine,
                (entity, triggerKey) -> Optional.of(trigger("on_damage")));

        listener.recordDamage(damaged, damager);

        assertThat(executed).isEmpty();
    }

    @Test
    void recordsPlayerProjectileDamage() {
        PlayerDamageTracker tracker = new PlayerDamageTracker(100);
        MobDamageListener listener = new MobDamageListener(tracker, () -> 75);
        LivingEntity damaged = mockLivingEntity(UUID.randomUUID());
        Player player = mockPlayer(UUID.randomUUID());
        Projectile projectile = mock(Projectile.class);
        when(projectile.getShooter()).thenReturn(player);

        listener.recordDamage(damaged, projectile);

        assertThat(tracker.lastPlayer(damaged.getUniqueId(), 75)).contains(player.getUniqueId());
    }

    @Test
    void ignoresNonPlayerDamage() {
        PlayerDamageTracker tracker = new PlayerDamageTracker(100);
        MobDamageListener listener = new MobDamageListener(tracker, () -> 75);
        LivingEntity damaged = mockLivingEntity(UUID.randomUUID());
        Entity damager = mock(Entity.class);

        listener.recordDamage(damaged, damager);

        assertThat(tracker.lastPlayer(damaged.getUniqueId(), 75)).isEmpty();
    }

    @Test
    void extractsOnlyPlayerSources() {
        Player player = mockPlayer(UUID.randomUUID());
        Projectile projectile = mock(Projectile.class);
        Projectile nonPlayerProjectile = mock(Projectile.class);
        Entity entity = mock(Entity.class);
        ProjectileSource nonPlayerSource = mock(ProjectileSource.class);
        when(projectile.getShooter()).thenReturn(player);
        when(nonPlayerProjectile.getShooter()).thenReturn(nonPlayerSource);

        assertThat(MobDamageListener.playerSource(player)).contains(player);
        assertThat(MobDamageListener.playerSource(projectile)).contains(player);
        assertThat(MobDamageListener.playerSource(nonPlayerProjectile)).isEqualTo(Optional.empty());
        assertThat(MobDamageListener.playerSource(entity)).isEqualTo(Optional.empty());
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

    private static TriggerDefinition trigger(String key) {
        return new TriggerDefinition(key, 1.0, 0, 0,
                List.of(new ActionDefinition("hostile_target", Map.of())));
    }
}
