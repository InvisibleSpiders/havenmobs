package com.nick.mobrarity.effect;

import java.util.Map;
import java.util.Optional;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public record TriggerContext(Map<String, Object> values) {
    private static final TriggerContext EMPTY = new TriggerContext(Map.of());
    private static final String ENTITY_KEY = "entity";
    private static final String PLAYER_KEY = "player";

    public TriggerContext {
        values = Map.copyOf(values);
    }

    public static TriggerContext empty() {
        return EMPTY;
    }

    public static TriggerContext forEntity(LivingEntity entity, Player player) {
        if (player == null) {
            return new TriggerContext(Map.of(ENTITY_KEY, entity));
        }
        return new TriggerContext(Map.of(ENTITY_KEY, entity, PLAYER_KEY, player));
    }

    public Optional<LivingEntity> entity() {
        Object value = values.get(ENTITY_KEY);
        if (value instanceof LivingEntity entity) {
            return Optional.of(entity);
        }
        return Optional.empty();
    }

    public Optional<Player> player() {
        Object value = values.get(PLAYER_KEY);
        if (value instanceof Player player) {
            return Optional.of(player);
        }
        return Optional.empty();
    }
}
