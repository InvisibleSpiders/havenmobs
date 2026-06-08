package com.nick.mobrarity.command;

import java.util.Optional;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

@FunctionalInterface
public interface TargetResolver {
    Optional<LivingEntity> target(Player player);
}
