package com.nick.mobrarity.listener;

import com.nick.mobrarity.effect.EffectEngine;
import com.nick.mobrarity.effect.TriggerContext;
import com.nick.mobrarity.effect.TriggerDefinition;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.UUID;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;

public final class MobDeathListener implements Listener {
    private final PlayerDamageTracker tracker;
    private final EffectEngine effectEngine;
    private final Function<LivingEntity, Optional<TriggerDefinition>> deathTriggerLookup;
    private final LongSupplier currentTick;
    private final Function<UUID, Optional<Player>> playerLookup;

    public MobDeathListener(
            PlayerDamageTracker tracker,
            EffectEngine effectEngine,
            Function<LivingEntity, Optional<TriggerDefinition>> deathTriggerLookup,
            LongSupplier currentTick) {
        this(tracker, effectEngine, deathTriggerLookup, currentTick, playerId -> Optional.empty());
    }

    public MobDeathListener(
            PlayerDamageTracker tracker,
            EffectEngine effectEngine,
            Function<LivingEntity, Optional<TriggerDefinition>> deathTriggerLookup,
            LongSupplier currentTick,
            Function<UUID, Optional<Player>> playerLookup) {
        this.tracker = Objects.requireNonNull(tracker, "tracker");
        this.effectEngine = Objects.requireNonNull(effectEngine, "effectEngine");
        this.deathTriggerLookup = Objects.requireNonNull(deathTriggerLookup, "deathTriggerLookup");
        this.currentTick = Objects.requireNonNull(currentTick, "currentTick");
        this.playerLookup = Objects.requireNonNull(playerLookup, "playerLookup");
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        processDeath(event.getEntity(), event.getEntity().getLastDamageCause());
    }

    void processDeath(LivingEntity entity, EntityDamageEvent fatalCause) {
        if (!(fatalCause instanceof EntityDamageByEntityEvent damageByEntity)
                || MobDamageListener.playerSource(damageByEntity.getDamager()).isEmpty()) {
            return;
        }
        Optional<UUID> trackedPlayerId = tracker.lastPlayer(entity.getUniqueId(), currentTick.getAsLong());
        if (trackedPlayerId.isEmpty()) {
            return;
        }
        Optional<Player> trackedPlayer = playerLookup.apply(trackedPlayerId.get());
        tracker.remove(entity.getUniqueId());
        if (trackedPlayer.isEmpty()) {
            return;
        }
        deathTriggerLookup.apply(entity).ifPresent(trigger ->
                effectEngine.run(trigger, TriggerContext.forEntity(entity, trackedPlayer.get())));
    }
}
