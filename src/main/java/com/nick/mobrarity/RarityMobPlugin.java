package com.nick.mobrarity;

import com.nick.mobrarity.command.MobRarityCommand;
import com.nick.mobrarity.command.MobRarityTabCompleter;
import com.nick.mobrarity.command.BukkitTargetResolver;
import com.nick.mobrarity.command.MobRarityAdminService;
import com.nick.mobrarity.command.AdminCommandResult;
import com.nick.mobrarity.effect.EffectEngine;
import com.nick.mobrarity.config.ConfigService;
import com.nick.mobrarity.config.ConfigSnapshot;
import com.nick.mobrarity.config.ConfigValidationException;
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
import com.nick.mobrarity.tag.MobTagService;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.random.RandomGenerator;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

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
        PlayerDamageTracker playerDamageTracker = new PlayerDamageTracker(20L * 30L);
        EffectEngine effectEngine = new EffectEngine(type -> Optional.empty(), Math::random);

        getServer().getPluginManager().registerEvents(
                new MobSpawnListener(() -> runtimeConfig.get().mobProfiles(), spawnRarityService, mobLevelService, mobTagService),
                this);
        getServer().getPluginManager().registerEvents(
                new MobDamageListener(playerDamageTracker, Bukkit::getCurrentTick),
                this);
        getServer().getPluginManager().registerEvents(
                new MobDeathListener(playerDamageTracker, effectEngine, entity -> Optional.empty(), Bukkit::getCurrentTick),
                this);
        getServer().getPluginManager().registerEvents(new MobActivityListener(), this);
        PluginCommand mobRarityCommand = getCommand("mobrarity");
        if (mobRarityCommand != null) {
            MobRarityAdminService adminService = new MobRarityAdminService(
                    runtimeConfig::get,
                    mobTagService,
                    new BukkitTargetResolver(16));
            mobRarityCommand.setExecutor(new MobRarityCommand(
                    adminService,
                    () -> reloadRuntimeConfig(dataFolder, runtimeConfig),
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

    private List<String> onlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .sorted()
                .toList();
    }
}
