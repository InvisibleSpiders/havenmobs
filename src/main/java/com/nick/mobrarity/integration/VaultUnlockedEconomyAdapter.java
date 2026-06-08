package com.nick.mobrarity.integration;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;

public final class VaultUnlockedEconomyAdapter implements EconomyAdapter {
    private final Economy economy;

    public VaultUnlockedEconomyAdapter(Economy economy) {
        this.economy = economy;
    }

    @Override
    public boolean available() {
        return economy != null;
    }

    @Override
    public void deposit(Player player, double amount) {
        if (player == null || economy == null || amount <= 0 || !Double.isFinite(amount)) {
            return;
        }
        economy.depositPlayer(player, amount);
    }
}
