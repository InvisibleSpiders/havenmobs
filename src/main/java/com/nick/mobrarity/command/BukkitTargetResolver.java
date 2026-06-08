package com.nick.mobrarity.command;

import java.util.Optional;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

public final class BukkitTargetResolver implements TargetResolver {
    private final int maxDistance;

    public BukkitTargetResolver(int maxDistance) {
        if (maxDistance < 1) {
            throw new IllegalArgumentException("maxDistance must be at least 1");
        }
        this.maxDistance = maxDistance;
    }

    @Override
    public Optional<LivingEntity> target(Player player) {
        Location eyeLocation = player.getEyeLocation();
        World world = eyeLocation.getWorld();
        if (world == null) {
            return Optional.empty();
        }

        RayTraceResult result = world.rayTraceEntities(
                eyeLocation,
                eyeLocation.getDirection(),
                maxDistance,
                0.25,
                entity -> entity instanceof LivingEntity && !entity.getUniqueId().equals(player.getUniqueId()));
        if (result == null) {
            return Optional.empty();
        }

        Entity hitEntity = result.getHitEntity();
        if (hitEntity instanceof LivingEntity livingEntity) {
            return Optional.of(livingEntity);
        }
        return Optional.empty();
    }
}
