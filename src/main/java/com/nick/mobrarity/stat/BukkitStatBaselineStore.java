package com.nick.mobrarity.stat;

import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class BukkitStatBaselineStore implements StatBaselineStore {
    private final NamespacedKey key;

    public BukkitStatBaselineStore(Plugin plugin) {
        this.key = new NamespacedKey(plugin, "stat_baselines");
    }

    @Override
    public Map<String, Double> baselines(LivingEntity entity) {
        String stored = container(entity).get(key, PersistentDataType.STRING);
        if (stored == null || stored.isBlank()) {
            return Map.of();
        }
        Map<String, Double> baselines = new LinkedHashMap<>();
        for (String entry : stored.split(";")) {
            String[] parts = entry.split("=", 2);
            if (parts.length != 2) {
                continue;
            }
            try {
                baselines.put(parts[0], Double.parseDouble(parts[1]));
            } catch (NumberFormatException ignored) {
                // Ignore malformed stored values; future scaling can replace missing baselines.
            }
        }
        return baselines;
    }

    @Override
    public void storeBaseline(LivingEntity entity, String statKey, double value) {
        Map<String, Double> baselines = new LinkedHashMap<>(baselines(entity));
        if (baselines.containsKey(statKey)) {
            return;
        }
        baselines.put(statKey, value);
        container(entity).set(key, PersistentDataType.STRING, serialize(baselines));
    }

    @Override
    public void clear(LivingEntity entity) {
        container(entity).remove(key);
    }

    private static PersistentDataContainer container(LivingEntity entity) {
        return entity.getPersistentDataContainer();
    }

    private static String serialize(Map<String, Double> baselines) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Double> entry : baselines.entrySet()) {
            if (!builder.isEmpty()) {
                builder.append(';');
            }
            builder.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return builder.toString();
    }
}
