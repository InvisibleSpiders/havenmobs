package com.nick.mobrarity.listener;

import java.util.Objects;
import java.util.Optional;
import java.util.function.LongSupplier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public final class MobDamageListener implements Listener {
    private final PlayerDamageTracker tracker;
    private final LongSupplier currentTick;

    public MobDamageListener(PlayerDamageTracker tracker, LongSupplier currentTick) {
        this.tracker = Objects.requireNonNull(tracker, "tracker");
        this.currentTick = Objects.requireNonNull(currentTick, "currentTick");
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof LivingEntity damaged) {
            recordDamage(damaged, event.getDamager());
        }
    }

    void recordDamage(LivingEntity damaged, Entity damager) {
        playerSource(damager).ifPresent(player -> tracker.record(
                damaged.getUniqueId(),
                player.getUniqueId(),
                currentTick.getAsLong()));
    }

    static Optional<Player> playerSource(Entity damager) {
        if (damager instanceof Player player) {
            return Optional.of(player);
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return Optional.of(player);
        }
        return Optional.empty();
    }
}
