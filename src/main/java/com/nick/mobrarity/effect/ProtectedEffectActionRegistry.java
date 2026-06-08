package com.nick.mobrarity.effect;

import com.nick.mobrarity.integration.ProtectionAdapter;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public final class ProtectedEffectActionRegistry implements EffectActionRegistry {
    private static final String CLAIM_ACTION_KEY = "claim-action";
    private static final String HOSTILE_TARGET_ACTION = "hostile_target";
    private static final String MOB_GRIEFING_CLAIM_ACTION = "mob_griefing";

    private final EffectActionRegistry delegate;
    private final ProtectionAdapter protectionAdapter;

    public ProtectedEffectActionRegistry(EffectActionRegistry delegate, ProtectionAdapter protectionAdapter) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.protectionAdapter = Objects.requireNonNull(protectionAdapter, "protectionAdapter");
    }

    @Override
    public Optional<EffectAction> action(String type) {
        return delegate.action(type)
                .map(effectAction -> (action, context) -> {
                    if (canRun(action, context)) {
                        effectAction.execute(action, context);
                    }
                });
    }

    private boolean canRun(ActionDefinition action, TriggerContext context) {
        Optional<LivingEntity> entity = context.entity();
        if (entity.isEmpty()) {
            return true;
        }
        Location location = entity.get().getLocation();
        Player player = context.player().orElse(null);
        return protectionAdapter.canRun(player, location, claimAction(action), action.type());
    }

    private static String claimAction(ActionDefinition action) {
        Object configured = action.values().get(CLAIM_ACTION_KEY);
        if (configured instanceof String value && !value.isBlank()) {
            return value;
        }
        if (HOSTILE_TARGET_ACTION.equals(action.type())) {
            return MOB_GRIEFING_CLAIM_ACTION;
        }
        return action.type();
    }
}
