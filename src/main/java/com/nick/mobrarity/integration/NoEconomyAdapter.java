package com.nick.mobrarity.integration;

import org.bukkit.entity.Player;

public final class NoEconomyAdapter implements EconomyAdapter {
    @Override
    public boolean available() {
        return false;
    }

    @Override
    public void deposit(Player player, double amount) {
    }
}
