package com.nick.mobrarity.integration;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class LandClaimsProtectionAdapter implements ProtectionAdapter {
    private static final String BYPASS_PERMISSION = "mobrarity.bypass.claim-check";

    private final Object landClaimsApi;
    private final ProtectionFallbackPolicy fallbackPolicy;

    public LandClaimsProtectionAdapter(Object landClaimsApi, ProtectionFallbackPolicy fallbackPolicy) {
        this.landClaimsApi = landClaimsApi;
        this.fallbackPolicy = fallbackPolicy;
    }

    @Override
    public boolean canRun(Player player, Location location, String actionKey, String actionType) {
        if (landClaimsApi == null) {
            return fallbackPolicy.allowsMissingService(actionType);
        }
        if (player != null && player.hasPermission(BYPASS_PERMISSION)) {
            return true;
        }

        try {
            Method method = landClaimsApi.getClass().getMethod("canInteract", Player.class, Location.class, String.class);
            Object result = method.invoke(landClaimsApi, player, location, actionKey);
            return Boolean.TRUE.equals(result);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | RuntimeException e) {
            return fallbackPolicy.allowsMissingService(actionType);
        }
    }
}
