package com.nick.mobrarity.effect;

import java.util.Optional;

@FunctionalInterface
public interface EffectActionRegistry {
    Optional<EffectAction> action(String type);
}
