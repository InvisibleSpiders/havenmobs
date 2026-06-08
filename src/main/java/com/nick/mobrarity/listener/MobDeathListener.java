package com.nick.mobrarity.listener;

import com.nick.mobrarity.effect.EffectEngine;
import com.nick.mobrarity.effect.TriggerContext;
import com.nick.mobrarity.effect.TriggerDefinition;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.LongSupplier;
import org.bukkit.entity.LivingEntity;
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

    public MobDeathListener(
            PlayerDamageTracker tracker,
            EffectEngine effectEngine,
            Function<LivingEntity, Optional<TriggerDefinition>> deathTriggerLookup,
            LongSupplier currentTick) {
        this.tracker = Objects.requireNonNull(tracker, "tracker");
        this.effectEngine = Objects.requireNonNull(effectEngine, "effectEngine");
        this.deathTriggerLookup = Objects.requireNonNull(deathTriggerLookup, "deathTriggerLookup");
        this.currentTick = Objects.requireNonNull(currentTick, "currentTick");
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
        if (tracker.lastPlayer(entity.getUniqueId(), currentTick.getAsLong()).isEmpty()) {
            return;
        }
        deathTriggerLookup.apply(entity).ifPresent(trigger -> effectEngine.run(trigger, TriggerContext.empty()));
    }
}
