package com.nick.mobrarity.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nick.landclaims.api.LandClaimsApi;
import com.nick.landclaims.api.claim.ClaimView;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

final class LandClaimsProtectionAdapterTest {
    @Test
    void missingApiUsesFallbackPolicy() {
        LandClaimsProtectionAdapter adapter =
                new LandClaimsProtectionAdapter(null, ProtectionFallbackPolicy.DENY_PROTECTED_EFFECTS);
        Player player = mock(Player.class);
        Location location = mock(Location.class);

        assertThat(adapter.canRun(player, location, "announce", "message")).isTrue();
        assertThat(adapter.canRun(player, location, "drop", "item_drop")).isFalse();
    }

    @Test
    void bypassPermissionAllowsWithoutCallingApi() {
        FakeLandClaimsApi api = new FakeLandClaimsApi(false);
        LandClaimsProtectionAdapter adapter =
                new LandClaimsProtectionAdapter(api, ProtectionFallbackPolicy.DENY_ALL_EFFECTS);
        Player player = mock(Player.class);
        Location location = mock(Location.class);
        when(player.hasPermission("mobrarity.bypass.claim-check")).thenReturn(true);

        assertThat(adapter.canRun(player, location, "drop", "item_drop")).isTrue();
        assertThat(api.calls()).isZero();
    }

    @Test
    void invokesLandClaimsApiWhenPresent() {
        FakeLandClaimsApi api = new FakeLandClaimsApi(true);
        LandClaimsProtectionAdapter adapter =
                new LandClaimsProtectionAdapter(api, ProtectionFallbackPolicy.DENY_ALL_EFFECTS);
        Player player = mock(Player.class);
        Location location = mock(Location.class);

        assertThat(adapter.canRun(player, location, "drop", "item_drop")).isTrue();
        assertThat(api.calls()).isOne();
    }

    @Test
    void returnsFalseWhenLandClaimsApiDeniesInteraction() {
        FakeLandClaimsApi api = new FakeLandClaimsApi(false);
        LandClaimsProtectionAdapter adapter =
                new LandClaimsProtectionAdapter(api, ProtectionFallbackPolicy.ALLOW);
        Player player = mock(Player.class);
        Location location = mock(Location.class);

        assertThat(adapter.canRun(player, location, "drop", "item_drop")).isFalse();
        assertThat(api.calls()).isOne();
    }

    @Test
    void apiFailureFallsBackWithoutThrowing() {
        ThrowingLandClaimsApi api = new ThrowingLandClaimsApi();
        LandClaimsProtectionAdapter adapter =
                new LandClaimsProtectionAdapter(api, ProtectionFallbackPolicy.DENY_PROTECTED_EFFECTS);
        Player player = mock(Player.class);
        Location location = mock(Location.class);

        assertThat(adapter.canRun(player, location, "announce", "message")).isTrue();
        assertThat(adapter.canRun(player, location, "drop", "item_drop")).isFalse();
        assertThat(api.calls()).isEqualTo(2);
    }

    private abstract static class TestLandClaimsApi implements LandClaimsApi {
        @Override
        public Optional<ClaimView> getClaimAt(Location location) {
            return Optional.empty();
        }

        @Override
        public Optional<ClaimView> getClaimById(UUID claimId) {
            return Optional.empty();
        }

        @Override
        public List<ClaimView> getClaimsOwnedBy(UUID playerId) {
            return List.of();
        }

        @Override
        public boolean canBuild(Player player, Location location) {
            return false;
        }
    }

    public static final class FakeLandClaimsApi extends TestLandClaimsApi {
        private final boolean result;
        private int calls;

        private FakeLandClaimsApi(boolean result) {
            this.result = result;
        }

        public boolean canInteract(Player player, Location location, String actionKey) {
            calls++;
            return result;
        }

        int calls() {
            return calls;
        }
    }

    public static final class ThrowingLandClaimsApi extends TestLandClaimsApi {
        private int calls;

        @Override
        public boolean canInteract(Player player, Location location, String actionKey) {
            calls++;
            throw new IllegalStateException("reflection target failed");
        }

        int calls() {
            return calls;
        }
    }
}
