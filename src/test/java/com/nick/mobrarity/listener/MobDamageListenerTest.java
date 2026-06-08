package com.nick.mobrarity.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
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
}
