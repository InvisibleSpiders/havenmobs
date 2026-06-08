package com.nick.mobrarity.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

final class EconomyAdapterTest {
    @Test
    void noEconomyIsUnavailableAndNoOps() {
        NoEconomyAdapter adapter = new NoEconomyAdapter();
        Player player = mock(Player.class);

        adapter.deposit(player, 10.0);

        assertThat(adapter.available()).isFalse();
    }

    @Test
    void vaultAdapterWithNullEconomyIsUnavailableAndNoOps() {
        VaultUnlockedEconomyAdapter adapter = new VaultUnlockedEconomyAdapter(null);
        Player player = mock(Player.class);

        adapter.deposit(player, 10.0);

        assertThat(adapter.available()).isFalse();
    }

    @Test
    void vaultAdapterDepositsPositiveAmount() {
        Economy economy = mock(Economy.class);
        Player player = mock(Player.class);
        VaultUnlockedEconomyAdapter adapter = new VaultUnlockedEconomyAdapter(economy);

        adapter.deposit(player, 10.0);

        assertThat(adapter.available()).isTrue();
        verify(economy).depositPlayer(player, 10.0);
    }

    @Test
    void vaultAdapterNoOpsForZeroAmount() {
        Economy economy = mock(Economy.class);
        Player player = mock(Player.class);
        VaultUnlockedEconomyAdapter adapter = new VaultUnlockedEconomyAdapter(economy);

        adapter.deposit(player, 0.0);

        verifyNoInteractions(economy);
    }

    @Test
    void vaultAdapterNoOpsForNegativeAmount() {
        Economy economy = mock(Economy.class);
        Player player = mock(Player.class);
        VaultUnlockedEconomyAdapter adapter = new VaultUnlockedEconomyAdapter(economy);

        adapter.deposit(player, -1.0);

        assertThat(adapter.available()).isTrue();
        verifyNoInteractions(economy);
    }

    @Test
    void vaultAdapterNoOpsForNullPlayer() {
        Economy economy = mock(Economy.class);
        VaultUnlockedEconomyAdapter adapter = new VaultUnlockedEconomyAdapter(economy);

        adapter.deposit(null, 10.0);

        verifyNoInteractions(economy);
    }

    @Test
    void vaultAdapterNoOpsForNanAmount() {
        Economy economy = mock(Economy.class);
        Player player = mock(Player.class);
        VaultUnlockedEconomyAdapter adapter = new VaultUnlockedEconomyAdapter(economy);

        adapter.deposit(player, Double.NaN);

        verifyNoInteractions(economy);
    }

    @Test
    void vaultAdapterNoOpsForInfiniteAmount() {
        Economy economy = mock(Economy.class);
        Player player = mock(Player.class);
        VaultUnlockedEconomyAdapter adapter = new VaultUnlockedEconomyAdapter(economy);

        adapter.deposit(player, Double.POSITIVE_INFINITY);

        verifyNoInteractions(economy);
    }
}
