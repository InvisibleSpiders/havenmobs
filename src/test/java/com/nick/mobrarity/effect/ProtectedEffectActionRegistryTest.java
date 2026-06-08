package com.nick.mobrarity.effect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nick.mobrarity.integration.ProtectionAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

final class ProtectedEffectActionRegistryTest {
    @Test
    void skipsProtectedActionWhenClaimsDenyIt() {
        List<String> executed = new ArrayList<>();
        ProtectionAdapter protectionAdapter = (player, location, actionKey, actionType) -> false;
        ProtectedEffectActionRegistry registry = new ProtectedEffectActionRegistry(
                type -> Optional.of((action, context) -> executed.add(type)),
                protectionAdapter);
        EffectEngine engine = new EffectEngine(registry, () -> 0.0);
        Player player = mock(Player.class);
        LivingEntity entity = entityAt(mock(Location.class));

        engine.run(trigger(new ActionDefinition("item_drop", Map.of())),
                TriggerContext.forEntity(entity, player));

        assertThat(executed).isEmpty();
    }

    @Test
    void passesConfiguredClaimActionToProtectionAdapter() {
        List<String> claimActions = new ArrayList<>();
        ProtectionAdapter protectionAdapter = (player, location, actionKey, actionType) -> {
            claimActions.add(actionKey);
            return true;
        };
        ProtectedEffectActionRegistry registry = new ProtectedEffectActionRegistry(
                type -> Optional.of((action, context) -> {
                }),
                protectionAdapter);
        EffectEngine engine = new EffectEngine(registry, () -> 0.0);
        Player player = mock(Player.class);
        LivingEntity entity = entityAt(mock(Location.class));

        engine.run(trigger(new ActionDefinition("hostile_target", Map.of("claim-action", "mob_griefing"))),
                TriggerContext.forEntity(entity, player));

        assertThat(claimActions).containsExactly("mob_griefing");
    }

    @Test
    void hostileTargetDefaultsToMobGriefingClaimAction() {
        List<String> claimActions = new ArrayList<>();
        ProtectionAdapter protectionAdapter = (player, location, actionKey, actionType) -> {
            claimActions.add(actionKey);
            return true;
        };
        ProtectedEffectActionRegistry registry = new ProtectedEffectActionRegistry(
                type -> Optional.of((action, context) -> {
                }),
                protectionAdapter);
        EffectEngine engine = new EffectEngine(registry, () -> 0.0);
        Player player = mock(Player.class);
        LivingEntity entity = entityAt(mock(Location.class));

        engine.run(trigger(new ActionDefinition("hostile_target", Map.of())),
                TriggerContext.forEntity(entity, player));

        assertThat(claimActions).containsExactly("mob_griefing");
    }

    private static TriggerDefinition trigger(ActionDefinition action) {
        return new TriggerDefinition("on_damage", 1.0, 0, 0, List.of(action));
    }

    private static LivingEntity entityAt(Location location) {
        LivingEntity entity = mock(LivingEntity.class);
        when(entity.getLocation()).thenReturn(location);
        return entity;
    }
}
