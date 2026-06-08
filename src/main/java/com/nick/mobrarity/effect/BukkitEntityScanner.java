package com.nick.mobrarity.effect;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public final class BukkitEntityScanner {
    public List<LivingEntity> livingEntities() {
        return Bukkit.getWorlds().stream()
                .flatMap(world -> world.getLivingEntities().stream())
                .toList();
    }

    public List<Player> onlinePlayers() {
        return new ArrayList<>(Bukkit.getOnlinePlayers());
    }
}
