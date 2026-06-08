package com.nick.mobrarity.effect;

import java.util.Objects;
import java.util.function.DoubleSupplier;

public final class EffectEngine {
    private final EffectActionRegistry registry;
    private final DoubleSupplier random;

    public EffectEngine(EffectActionRegistry registry, DoubleSupplier random) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.random = Objects.requireNonNull(random, "random");
    }

    public void run(TriggerDefinition trigger, TriggerContext context) {
        Objects.requireNonNull(trigger, "trigger");
        Objects.requireNonNull(context, "context");
        if (random.getAsDouble() >= trigger.chance()) {
            return;
        }
        for (ActionDefinition action : trigger.actions()) {
            registry.action(action.type()).ifPresent(effectAction -> effectAction.execute(action, context));
        }
    }
}
