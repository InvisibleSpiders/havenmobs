package com.nick.mobrarity.effect;

import com.nick.mobrarity.integration.ProtectionAdapter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public final class AuraTickService {
    private static final String AURA_TRIGGER = "on_aura_tick";

    private final Supplier<List<LivingEntity>> mobSupplier;
    private final Supplier<List<Player>> playerSupplier;
    private final TriggerLookup triggerLookup;
    private final ProtectionAdapter protectionAdapter;
    private final EffectEngine effectEngine;
    private final Map<UUID, Long> lastRunTicks = new ConcurrentHashMap<>();

    public AuraTickService(
            Supplier<List<LivingEntity>> mobSupplier,
            Supplier<List<Player>> playerSupplier,
            TriggerLookup triggerLookup,
            ProtectionAdapter protectionAdapter,
            EffectEngine effectEngine) {
        this.mobSupplier = Objects.requireNonNull(mobSupplier, "mobSupplier");
        this.playerSupplier = Objects.requireNonNull(playerSupplier, "playerSupplier");
        this.triggerLookup = Objects.requireNonNull(triggerLookup, "triggerLookup");
        this.protectionAdapter = Objects.requireNonNull(protectionAdapter, "protectionAdapter");
        this.effectEngine = Objects.requireNonNull(effectEngine, "effectEngine");
    }

    public void tick(long currentTick) {
        for (LivingEntity mob : mobSupplier.get()) {
            if (mob == null || mob.isDead() || !mob.isValid()) {
                continue;
            }
            Optional<TriggerDefinition> trigger = triggerLookup.trigger(mob, AURA_TRIGGER);
            if (trigger.isEmpty() || !intervalElapsed(mob, trigger.get(), currentTick)) {
                continue;
            }
            runForNearbyPlayers(mob, trigger.get());
            lastRunTicks.put(mob.getUniqueId(), currentTick);
        }
    }

    private boolean intervalElapsed(LivingEntity mob, TriggerDefinition trigger, long currentTick) {
        long interval = Math.max(1L, trigger.intervalTicks());
        Long lastRun = lastRunTicks.get(mob.getUniqueId());
        return lastRun == null ? currentTick >= interval : currentTick - lastRun >= interval;
    }

    private void runForNearbyPlayers(LivingEntity mob, TriggerDefinition trigger) {
        Location mobLocation = mob.getLocation();
        double radius = Math.max(0.0, trigger.radius());
        double radiusSquared = radius * radius;
        for (Player player : playerSupplier.get()) {
            if (player == null || !sameWorld(mob, player) || player.getLocation().distanceSquared(mobLocation) > radiusSquared) {
                continue;
            }
            TriggerDefinition allowedTrigger = allowedActions(trigger, player, mobLocation);
            if (!allowedTrigger.actions().isEmpty()) {
                effectEngine.run(allowedTrigger, TriggerContext.forEntity(mob, player));
            }
        }
    }

    private TriggerDefinition allowedActions(TriggerDefinition trigger, Player player, Location mobLocation) {
        List<ActionDefinition> allowedActions = trigger.actions().stream()
                .filter(action -> protectionAdapter.canRun(player, mobLocation, trigger.key(), action.type()))
                .toList();
        return new TriggerDefinition(
                trigger.key(),
                trigger.chance(),
                trigger.intervalTicks(),
                trigger.radius(),
                allowedActions);
    }

    private static boolean sameWorld(LivingEntity mob, Player player) {
        return mob.getWorld() != null && mob.getWorld().equals(player.getWorld());
    }

    @FunctionalInterface
    public interface TriggerLookup {
        Optional<TriggerDefinition> trigger(LivingEntity entity, String triggerKey);
    }
}
