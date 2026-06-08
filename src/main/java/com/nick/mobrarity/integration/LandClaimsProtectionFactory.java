package com.nick.mobrarity.integration;

import com.nick.landclaims.api.LandClaimsApi;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class LandClaimsProtectionFactory {
    private LandClaimsProtectionFactory() {
    }

    public static ProtectionAdapter load(JavaPlugin plugin, ProtectionFallbackPolicy fallbackPolicy) {
        RegisteredServiceProvider<LandClaimsApi> registration =
                plugin.getServer().getServicesManager().getRegistration(LandClaimsApi.class);
        LandClaimsApi api = registration == null ? null : registration.getProvider();
        return new LandClaimsProtectionAdapter(api, fallbackPolicy);
    }
}
