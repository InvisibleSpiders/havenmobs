package com.nick.mobrarity.listener;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

final class PlayerDamageTrackerTest {
    @Test
    void expiresOldDamage() {
        PlayerDamageTracker tracker = new PlayerDamageTracker(100);
        UUID entityId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();

        tracker.record(entityId, playerId, 50);

        assertThat(tracker.lastPlayer(entityId, 120)).contains(playerId);
        assertThat(tracker.lastPlayer(entityId, 151)).isEmpty();
    }

    @Test
    void unknownEntityHasNoLastPlayer() {
        PlayerDamageTracker tracker = new PlayerDamageTracker(100);

        assertThat(tracker.lastPlayer(UUID.randomUUID(), 10)).isEmpty();
    }

    @Test
    void removeClearsTrackedEntityDamage() {
        PlayerDamageTracker tracker = new PlayerDamageTracker(100);
        UUID entityId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        tracker.record(entityId, playerId, 50);

        tracker.remove(entityId);

        assertThat(tracker.lastPlayer(entityId, 60)).isEmpty();
    }

    @Test
    void pruneExpiredRemovesOldRecordsButKeepsFreshRecords() {
        PlayerDamageTracker tracker = new PlayerDamageTracker(100);
        UUID oldEntity = UUID.randomUUID();
        UUID freshEntity = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        tracker.record(oldEntity, playerId, 10);
        tracker.record(freshEntity, playerId, 90);

        tracker.pruneExpired(111);

        assertThat(tracker.lastPlayer(oldEntity, 111)).isEmpty();
        assertThat(tracker.lastPlayer(freshEntity, 111)).contains(playerId);
    }
}
