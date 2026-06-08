package com.nick.mobrarity.effect;

@FunctionalInterface
public interface EffectAction {
    void execute(ActionDefinition action, TriggerContext context);
}
