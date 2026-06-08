package com.nick.mobrarity.integration;

import com.nick.landclaims.api.LandClaimsApi;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class LandClaimsProtectionAdapter implements ProtectionAdapter {
    private static final String BYPASS_PERMISSION = "mobrarity.bypass.claim-check";

    private final LandClaimsApi landClaimsApi;
    private final ProtectionFallbackPolicy fallbackPolicy;

    public LandClaimsProtectionAdapter(LandClaimsApi landClaimsApi, ProtectionFallbackPolicy fallbackPolicy) {
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
            return landClaimsApi.canInteract(player, location, actionKey);
        } catch (RuntimeException e) {
            return fallbackPolicy.allowsMissingService(actionType);
        }
    }
}
