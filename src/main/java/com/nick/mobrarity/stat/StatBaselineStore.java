package com.nick.mobrarity.stat;

import java.util.Map;
import org.bukkit.entity.LivingEntity;

public interface StatBaselineStore {
    Map<String, Double> baselines(LivingEntity entity);

    void storeBaseline(LivingEntity entity, String statKey, double value);

    void clear(LivingEntity entity);
}
