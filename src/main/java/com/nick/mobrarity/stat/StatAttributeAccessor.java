package com.nick.mobrarity.stat;

import java.util.OptionalDouble;
import org.bukkit.entity.LivingEntity;

public interface StatAttributeAccessor {
    OptionalDouble baseValue(LivingEntity entity, String statKey);

    void setBaseValue(LivingEntity entity, String statKey, double value);

    void healToMax(LivingEntity entity);
}
