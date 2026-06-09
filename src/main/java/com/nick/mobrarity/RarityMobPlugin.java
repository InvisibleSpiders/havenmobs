package com.nick.mobrarity;

import com.nick.mobrarity.command.MobRarityCommand;
import com.nick.mobrarity.command.MobRarityTabCompleter;
import com.nick.mobrarity.command.BukkitTargetResolver;
import com.nick.mobrarity.command.MobRarityAdminService;
import com.nick.mobrarity.command.AdminCommandResult;
import com.nick.mobrarity.effect.AuraTickService;
import com.nick.mobrarity.effect.BukkitEntityScanner;
import com.nick.mobrarity.effect.BukkitEffectActionRegistry;
import com.nick.mobrarity.effect.EffectEngine;
import com.nick.mobrarity.effect.ProtectedEffectActionRegistry;
import com.nick.mobrarity.effect.RarityTriggerService;
import com.nick.mobrarity.config.ConfigService;
import com.nick.mobrarity.config.ConfigSnapshot;
import com.nick.mobrarity.config.ConfigValidationException;
import com.nick.mobrarity.integration.EconomyAdapter;
import com.nick.mobrarity.integration.LandClaimsProtectionFactory;
import com.nick.mobrarity.integration.MobRarityPlaceholderExpansion;
import com.nick.mobrarity.integration.NoEconomyAdapter;
import com.nick.mobrarity.integration.ProtectionAdapter;
import com.nick.mobrarity.integration.ProtectionFallbackPolicy;
import com.nick.mobrarity.integration.VaultUnlockedEconomyAdapter;
import com.nick.mobrarity.level.LevelSettings;
import com.nick.mobrarity.level.MobLevelService;
import com.nick.mobrarity.listener.MobActivityListener;
import com.nick.mobrarity.listener.MobDamageListener;
import com.nick.mobrarity.listener.MobDeathListener;
import com.nick.mobrarity.listener.MobSpawnListener;
import com.nick.mobrarity.listener.PlayerDamageTracker;
import com.nick.mobrarity.rarity.MobVariant;
import com.nick.mobrarity.rarity.SpawnRarityService;
import com.nick.mobrarity.rarity.WeightedSelector;
import com.nick.mobrarity.stat.BukkitStatAttributeAccessor;
import com.nick.mobrarity.stat.StatScalingService;
import com.nick.mobrarity.tag.MobTagService;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.random.RandomGenerator;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class RarityMobPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResourceIfMissing("tiers.yml");
        saveResourceIfMissing("mobs.yml");
        Path dataFolder = getDataFolder().toPath();
        ConfigSnapshot config = loadRuntimeConfig(dataFolder);
        AtomicReference<ConfigSnapshot> runtimeConfig = new AtomicReference<>(config);
        WeightedSelector<MobVariant> weightedSelector = new WeightedSelector<>(RandomGenerator.getDefault());
        SpawnRarityService spawnRarityService = new SpawnRarityService(weightedSelector::select);
        LevelSettings levelSettings = new LevelSettings(true, 250, 1, 100, true, 64, 0.15, 25);
        MobLevelService mobLevelService = new MobLevelService(levelSettings);
        MobTagService mobTagService = new MobTagService(this);
        registerPlaceholderExpansion(runtimeConfig, mobTagService);
        StatScalingService statScalingService = new StatScalingService(new BukkitStatAttributeAccessor());
        PlayerDamageTracker playerDamageTracker = new PlayerDamageTracker(20L * 30L);
        EconomyAdapter economyAdapter = loadEconomyAdapter();
        ProtectionAdapter protectionAdapter = loadProtectionAdapter();
        EffectEngine effectEngine = new EffectEngine(
                new ProtectedEffectActionRegistry(
                        new BukkitEffectActionRegistry(economyAdapter, RandomGenerator.getDefault()),
                        protectionAdapter),
                Math::random);
        RarityTriggerService triggerService = new RarityTriggerService(runtimeConfig::get, mobTagService);
        BukkitEntityScanner entityScanner = new BukkitEntityScanner();
        AuraTickService auraTickService = new AuraTickService(
                entityScanner::livingEntities,
                entityScanner::onlinePlayers,
                triggerService::trigger,
                protectionAdapter,
                effectEngine);
        Bukkit.getScheduler().runTaskTimer(this, () -> auraTickService.tick(Bukkit.getCurrentTick()), 20L, 20L);

        getServer().getPluginManager().registerEvents(
                new MobSpawnListener(runtimeConfig::get, spawnRarityService, mobLevelService, mobTagService, statScalingService),
                this);
        getServer().getPluginManager().registerEvents(
                new MobDamageListener(playerDamageTracker, Bukkit::getCurrentTick, effectEngine, triggerService::trigger),
                this);
        getServer().getPluginManager().registerEvents(
                new MobDeathListener(playerDamageTracker, effectEngine, entity -> triggerService.trigger(entity, "on_death"), Bukkit::getCurrentTick),
                this);
        getServer().getPluginManager().registerEvents(new MobActivityListener(effectEngine, triggerService::trigger), this);
        PluginCommand mobRarityCommand = getCommand("mobrarity");
        if (mobRarityCommand != null) {
            MobRarityAdminService adminService = new MobRarityAdminService(
                    runtimeConfig::get,
                    mobTagService,
                    new BukkitTargetResolver(16),
                    statScalingService);
            mobRarityCommand.setExecutor(new MobRarityCommand(
                    adminService,
                    () -> reloadRuntimeConfig(dataFolder, runtimeConfig),
                    () -> validateRuntimeConfig(dataFolder),
                    name -> Optional.ofNullable(Bukkit.getPlayerExact(name)),
                    this::onlinePlayerNames));
            mobRarityCommand.setTabCompleter(new MobRarityTabCompleter(
                    adminService::completions,
                    this::onlinePlayerNames));
        }
        getLogger().info("MobRarity enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("MobRarity disabled.");
    }

    private void saveResourceIfMissing(String name) {
        if (!getDataFolder().toPath().resolve(name).toFile().exists()) {
            saveResource(name, false);
        }
    }

    static ConfigSnapshot loadRuntimeConfig(Path dataFolder) {
        return ConfigService.fromFiles(
                dataFolder.resolve("tiers.yml"),
                dataFolder.resolve("mobs.yml")).snapshot();
    }

    private AdminCommandResult reloadRuntimeConfig(Path dataFolder, AtomicReference<ConfigSnapshot> runtimeConfig) {
        try {
            runtimeConfig.set(loadRuntimeConfig(dataFolder));
            return AdminCommandResult.success("Reloaded MobRarity config.");
        } catch (ConfigValidationException exception) {
            return AdminCommandResult.failure("MobRarity config reload failed: " + exception.getMessage());
        }
    }

    private AdminCommandResult validateRuntimeConfig(Path dataFolder) {
        try {
            ConfigSnapshot snapshot = loadRuntimeConfig(dataFolder);
            long variantCount = snapshot.mobProfiles().values().stream()
                    .mapToLong(profile -> profile.variants().size())
                    .sum();
            return AdminCommandResult.success("MobRarity config is valid: %d tiers, %d mobs, %d variants.".formatted(
                    snapshot.tiers().size(),
                    snapshot.mobProfiles().size(),
                    variantCount));
        } catch (ConfigValidationException exception) {
            return AdminCommandResult.failure("MobRarity config validation failed: " + exception.getMessage());
        }
    }

    private List<String> onlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .sorted()
                .toList();
    }

    private EconomyAdapter loadEconomyAdapter() {
        RegisteredServiceProvider<Economy> registration = getServer().getServicesManager().getRegistration(Economy.class);
        if (registration == null) {
            return new NoEconomyAdapter();
        }
        return new VaultUnlockedEconomyAdapter(registration.getProvider());
    }

    private void registerPlaceholderExpansion(
            AtomicReference<ConfigSnapshot> runtimeConfig,
            MobTagService mobTagService) {
        if (!getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return;
        }
        new MobRarityPlaceholderExpansion(
                runtimeConfig::get,
                mobTagService,
                new BukkitTargetResolver(32),
                getPluginMeta().getVersion())
                .register();
    }

    private ProtectionAdapter loadProtectionAdapter() {
        ProtectionFallbackPolicy fallbackPolicy = ProtectionFallbackPolicy.ALLOW;
        try {
            Class.forName("com.nick.landclaims.api.LandClaimsApi", false, getClass().getClassLoader());
            return LandClaimsProtectionFactory.load(this, fallbackPolicy);
        } catch (ClassNotFoundException | NoClassDefFoundError exception) {
            return (player, location, actionKey, actionType) -> fallbackPolicy.allowsMissingService(actionType);
        }
    }
}
