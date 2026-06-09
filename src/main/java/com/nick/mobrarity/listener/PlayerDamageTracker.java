package com.nick.mobrarity.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class PlayerDamageTracker {
    private final long expiryTicks;
    private final Map<UUID, DamageRecord> records = new HashMap<>();

    public PlayerDamageTracker(long expiryTicks) {
        this.expiryTicks = expiryTicks;
    }

    public void record(UUID entityId, UUID playerId, long currentTick) {
        records.put(
                Objects.requireNonNull(entityId, "entityId"),
                new DamageRecord(Objects.requireNonNull(playerId, "playerId"), currentTick));
    }

    public Optional<UUID> lastPlayer(UUID entityId, long currentTick) {
        DamageRecord record = records.get(entityId);
        if (record == null || currentTick - record.tick() > expiryTicks) {
            return Optional.empty();
        }
        return Optional.of(record.playerId());
    }

    public void remove(UUID entityId) {
        records.remove(Objects.requireNonNull(entityId, "entityId"));
    }

    public void pruneExpired(long currentTick) {
        records.entrySet().removeIf(entry -> currentTick - entry.getValue().tick() > expiryTicks);
    }

    private record DamageRecord(UUID playerId, long tick) {
    }
}
