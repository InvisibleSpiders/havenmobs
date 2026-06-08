package com.nick.mobrarity.integration;

import org.bukkit.entity.Player;

public interface EconomyAdapter {
    boolean available();

    void deposit(Player player, double amount);
}
