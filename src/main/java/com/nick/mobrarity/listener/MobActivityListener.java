package com.nick.mobrarity.listener;

import com.nick.mobrarity.effect.EffectEngine;
import com.nick.mobrarity.effect.TriggerContext;
import com.nick.mobrarity.effect.TriggerDefinition;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;

public final class MobActivityListener implements Listener {
    private final EffectEngine effectEngine;
    private final TriggerLookup triggerLookup;

    public MobActivityListener() {
        this(null, (entity, triggerKey) -> Optional.empty());
    }

    public MobActivityListener(EffectEngine effectEngine, TriggerLookup triggerLookup) {
        this.effectEngine = effectEngine;
        this.triggerLookup = Objects.requireNonNull(triggerLookup, "triggerLookup");
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerShearEntity(PlayerShearEntityEvent event) {
        Entity target = event.getEntity();
        if (!(target instanceof LivingEntity livingEntity) || effectEngine == null) {
            return;
        }
        triggerLookup.trigger(livingEntity, "on_shear")
                .ifPresent(trigger -> effectEngine.run(trigger, TriggerContext.forEntity(livingEntity, event.getPlayer())));
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityTame(EntityTameEvent event) {
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityBreed(EntityBreedEvent event) {
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
    }

    @FunctionalInterface
    public interface TriggerLookup {
        Optional<TriggerDefinition> trigger(LivingEntity entity, String triggerKey);
    }
}
