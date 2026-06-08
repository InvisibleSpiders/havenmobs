package com.nick.mobrarity.integration;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface ProtectionAdapter {
    boolean canRun(Player player, Location location, String actionKey, String actionType);
}
