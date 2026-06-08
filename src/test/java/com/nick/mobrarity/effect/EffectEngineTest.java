package com.nick.mobrarity.effect;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class EffectEngineTest {
    @Test
    void skipsActionWhenChanceFails() {
        List<String> executed = new ArrayList<>();
        EffectEngine engine = new EffectEngine(type -> Optional.of((action, context) -> executed.add(type)), () -> 0.99);

        engine.run(new TriggerDefinition("on_shear", 0.25, 0, 0,
                List.of(new ActionDefinition("message", Map.of()))), TriggerContext.empty());

        assertThat(executed).isEmpty();
    }

    @Test
    void executesConfiguredActionsWhenChancePasses() {
        List<String> executed = new ArrayList<>();
        EffectEngine engine = new EffectEngine(type -> Optional.of((action, context) -> executed.add(type)), () -> 0.0);

        engine.run(new TriggerDefinition("on_shear", 0.25, 0, 0,
                List.of(new ActionDefinition("message", Map.of()))), TriggerContext.empty());

        assertThat(executed).containsExactly("message");
    }

    @Test
    void executesMultipleActionsInOrder() {
        List<String> executed = new ArrayList<>();
        EffectEngine engine = new EffectEngine(type -> Optional.of((action, context) -> executed.add(type)), () -> 0.0);

        engine.run(new TriggerDefinition("on_death", 1.0, 0, 0, List.of(
                new ActionDefinition("message", Map.of()),
                new ActionDefinition("sound", Map.of()),
                new ActionDefinition("drop", Map.of())
        )), TriggerContext.empty());

        assertThat(executed).containsExactly("message", "sound", "drop");
    }

    @Test
    void skipsUnknownActionSafely() {
        List<String> executed = new ArrayList<>();
        EffectEngine engine = new EffectEngine(type -> "known".equals(type)
                ? Optional.of((action, context) -> executed.add(type))
                : Optional.empty(), () -> 0.0);

        engine.run(new TriggerDefinition("on_interact", 1.0, 0, 0, List.of(
                new ActionDefinition("missing", Map.of()),
                new ActionDefinition("known", Map.of())
        )), TriggerContext.empty());

        assertThat(executed).containsExactly("known");
    }

    @Test
    void passesActionDefinitionValuesToAction() {
        List<Map<String, Object>> values = new ArrayList<>();
        EffectEngine engine = new EffectEngine(type -> Optional.of((action, context) -> values.add(action.values())), () -> 0.0);

        engine.run(new TriggerDefinition("on_shear", 1.0, 0, 0,
                List.of(new ActionDefinition("item_drop", Map.of("material", "DIAMOND", "amount", 2)))),
                TriggerContext.empty());

        assertThat(values).containsExactly(Map.of("material", "DIAMOND", "amount", 2));
    }
}
