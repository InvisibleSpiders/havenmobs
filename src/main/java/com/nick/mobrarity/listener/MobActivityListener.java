package com.nick.mobrarity.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;

public final class MobActivityListener implements Listener {
    @EventHandler(ignoreCancelled = true)
    public void onPlayerShearEntity(PlayerShearEntityEvent event) {
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityTame(EntityTameEvent event) {
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityBreed(EntityBreedEvent event) {
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
    }
}
