package com.nick.mobrarity.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    void reflectionFailureFallsBackWithoutThrowing() {
        ThrowingLandClaimsApi api = new ThrowingLandClaimsApi();
        LandClaimsProtectionAdapter adapter =
                new LandClaimsProtectionAdapter(api, ProtectionFallbackPolicy.DENY_PROTECTED_EFFECTS);
        Player player = mock(Player.class);
        Location location = mock(Location.class);

        assertThat(adapter.canRun(player, location, "announce", "message")).isTrue();
        assertThat(adapter.canRun(player, location, "drop", "item_drop")).isFalse();
        assertThat(api.calls()).isEqualTo(2);
    }

    public static final class FakeLandClaimsApi {
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

    public static final class ThrowingLandClaimsApi {
        private int calls;

        public boolean canInteract(Player player, Location location, String actionKey) {
            calls++;
            throw new IllegalStateException("reflection target failed");
        }

        int calls() {
            return calls;
        }
    }
}
