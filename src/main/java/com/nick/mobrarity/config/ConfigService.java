package com.nick.mobrarity.config;

import com.nick.mobrarity.effect.ActionDefinition;
import com.nick.mobrarity.effect.TriggerDefinition;
import com.nick.mobrarity.rarity.MobProfile;
import com.nick.mobrarity.rarity.MobVariant;
import com.nick.mobrarity.rarity.RarityTier;
import com.nick.mobrarity.stat.StatModifier;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

public final class ConfigService {
    private final ConfigSnapshot snapshot;

    private ConfigService(ConfigSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    public static ConfigService fromResources() {
        YamlConfiguration tiersYaml = loadResource("tiers.yml");
        YamlConfiguration mobsYaml = loadResource("mobs.yml");

        return fromYaml(tiersYaml, mobsYaml);
    }

    public static ConfigService fromFiles(Path tiersPath, Path mobsPath) {
        YamlConfiguration tiersYaml = loadFile(tiersPath, "tiers.yml");
        YamlConfiguration mobsYaml = loadFile(mobsPath, "mobs.yml");

        return fromYaml(tiersYaml, mobsYaml);
    }

    public static ConfigService fromYaml(YamlConfiguration tiersYaml, YamlConfiguration mobsYaml) {
        Map<String, RarityTier> tiers = parseTiers(requiredSection(tiersYaml, "tiers", "tiers.yml"), "tiers", "tiers.yml");
        Map<EntityType, MobProfile> mobProfiles = parseMobProfiles(
                requiredSection(mobsYaml, "mobs", "mobs.yml"),
                "mobs",
                "mobs.yml",
                tiers.keySet());
        return new ConfigService(new ConfigSnapshot(tiers, mobProfiles));
    }

    public ConfigSnapshot snapshot() {
        return snapshot;
    }

    public static ValidationResult validateVariantTier(String tierKey, Set<String> knownTierKeys) {
        if (knownTierKeys.contains(tierKey)) {
            return ValidationResult.success();
        }
        return ValidationResult.invalid("Unknown tier '" + tierKey + "'.");
    }

    private static YamlConfiguration loadResource(String resourceName) {
        InputStream stream = ConfigService.class.getClassLoader().getResourceAsStream(resourceName);
        if (stream == null) {
            throw new ConfigValidationException("Missing required resource " + resourceName + ".");
        }
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (Exception exception) {
            throw new ConfigValidationException("Could not load " + resourceName + ": " + exception.getMessage());
        }
    }

    private static YamlConfiguration loadFile(Path path, String resourceName) {
        if (path == null) {
            throw new ConfigValidationException("Missing required file path for " + resourceName + ".");
        }
        if (!Files.isRegularFile(path)) {
            throw new ConfigValidationException("Missing required file " + path + " for " + resourceName + ".");
        }
        return YamlConfiguration.loadConfiguration(path.toFile());
    }

    private static ConfigurationSection requiredSection(YamlConfiguration yaml, String path, String resourceName) {
        ConfigurationSection section = yaml.getConfigurationSection(path);
        if (section == null) {
            throw new ConfigValidationException("Missing required section '" + path + "' in " + resourceName + ".");
        }
        return section;
    }

    private static Map<String, RarityTier> parseTiers(ConfigurationSection tiersSection, String path, String resourceName) {
        Map<String, RarityTier> tiers = new LinkedHashMap<>();
        for (String key : tiersSection.getKeys(false)) {
            String tierPath = path + "." + key;
            validateConfigKey(key, tierPath, resourceName);
            ConfigurationSection section = requiredChildSection(tiersSection, key, tierPath, resourceName);
            tiers.put(key, new RarityTier(
                    key,
                    section.getDouble("weight", 1.0),
                    section.getString("display-name"),
                    section.getString("display.nametag"),
                    parseStats(optionalChildSection(section, "stats", tierPath + ".stats", resourceName), tierPath + ".stats", resourceName),
                    parseTriggers(optionalChildSection(section, "triggers", tierPath + ".triggers", resourceName), tierPath + ".triggers", resourceName)));
        }
        return tiers;
    }

    private static Map<EntityType, MobProfile> parseMobProfiles(
            ConfigurationSection mobsSection,
            String path,
            String resourceName,
            Set<String> knownTierKeys) {
        Map<EntityType, MobProfile> profiles = new LinkedHashMap<>();
        for (String key : mobsSection.getKeys(false)) {
            String mobPath = path + "." + key;
            ConfigurationSection section = requiredChildSection(mobsSection, key, mobPath, resourceName);
            EntityType entityType = resolveEntityType(key, mobPath, resourceName);
            profiles.put(entityType, new MobProfile(
                    parseSpawnSources(optionalChildSection(section, "spawn-sources", mobPath + ".spawn-sources", resourceName), mobPath + ".spawn-sources", resourceName),
                    parseVariants(optionalChildSection(section, "variants", mobPath + ".variants", resourceName), mobPath + ".variants", resourceName, knownTierKeys)));
        }
        return profiles;
    }

    private static Map<SpawnReason, Boolean> parseSpawnSources(ConfigurationSection section, String path, String resourceName) {
        Map<SpawnReason, Boolean> spawnSources = new LinkedHashMap<>();
        if (section == null) {
            return spawnSources;
        }
        for (String key : section.getKeys(false)) {
            SpawnReason reason = resolveSpawnReason(key, path + "." + key, resourceName);
            spawnSources.put(reason, section.getBoolean(key));
        }
        return spawnSources;
    }

    private static SpawnReason resolveSpawnReason(String key, String path, String resourceName) {
        if ("SPAWN_EGG".equals(key)) {
            return SpawnReason.SPAWNER_EGG;
        }
        if ("PLUGIN".equals(key)) {
            return SpawnReason.CUSTOM;
        }
        try {
            return SpawnReason.valueOf(key);
        } catch (IllegalArgumentException ignored) {
            throw new ConfigValidationException("Unknown spawn source '" + key + "' at " + path + " in " + resourceName + ".");
        }
    }

    private static Map<String, MobVariant> parseVariants(
            ConfigurationSection variantsSection,
            String path,
            String resourceName,
            Set<String> knownTierKeys) {
        Map<String, MobVariant> variants = new LinkedHashMap<>();
        if (variantsSection == null) {
            return variants;
        }
        for (String key : variantsSection.getKeys(false)) {
            String variantPath = path + "." + key;
            validateConfigKey(key, variantPath, resourceName);
            ConfigurationSection section = requiredChildSection(variantsSection, key, variantPath, resourceName);
            String tierKey = section.getString("tier");
            ValidationResult validation = validateVariantTier(tierKey, knownTierKeys);
            if (!validation.valid()) {
                throw new ConfigValidationException(String.join(" ", validation.messages()) + " Path: " + variantPath + ".tier in " + resourceName + ".");
            }
            variants.put(key, new MobVariant(
                    key,
                    tierKey,
                    section.getDouble("weight", 1.0),
                    section.getString("display.nametag"),
                    parseStats(optionalChildSection(section, "stats", variantPath + ".stats", resourceName), variantPath + ".stats", resourceName),
                    parseTriggers(optionalChildSection(section, "triggers", variantPath + ".triggers", resourceName), variantPath + ".triggers", resourceName)));
        }
        return variants;
    }

    private static Map<String, StatModifier> parseStats(ConfigurationSection statsSection, String path, String resourceName) {
        Map<String, StatModifier> stats = new LinkedHashMap<>();
        if (statsSection == null) {
            return stats;
        }
        for (String key : statsSection.getKeys(false)) {
            String statPath = path + "." + key;
            validateConfigKey(key, statPath, resourceName);
            ConfigurationSection section = requiredChildSection(statsSection, key, statPath, resourceName);
            stats.put(key, new StatModifier(
                    section.getDouble("add", 0.0),
                    section.getDouble("multiply", 1.0),
                    section.getDouble("per-level", 0.0)));
        }
        return stats;
    }

    private static Map<String, TriggerDefinition> parseTriggers(ConfigurationSection triggersSection, String path, String resourceName) {
        Map<String, TriggerDefinition> triggers = new LinkedHashMap<>();
        if (triggersSection == null) {
            return triggers;
        }
        for (String key : triggersSection.getKeys(false)) {
            String triggerPath = path + "." + key;
            ConfigurationSection section = requiredChildSection(triggersSection, key, triggerPath, resourceName);
            triggers.put(key, new TriggerDefinition(
                    key,
                    section.getDouble("chance", 1.0),
                    section.getLong("interval-ticks", 0L),
                    section.getDouble("radius", 0.0),
                    parseActions(section, triggerPath + ".actions", resourceName)));
        }
        return triggers;
    }

    private static void validateConfigKey(String key, String path, String resourceName) {
        if (key == null || key.isBlank()) {
            throw new ConfigValidationException("Invalid config key at " + path + " in " + resourceName + ": key must not be blank.");
        }
        if (key.contains("|")) {
            throw new ConfigValidationException("Invalid config key at " + path + " in " + resourceName + ": key must not contain '|'.");
        }
    }

    private static List<ActionDefinition> parseActions(ConfigurationSection section, String path, String resourceName) {
        if (!section.contains("actions")) {
            return List.of();
        }
        List<?> actionEntries = section.getList("actions");
        if (actionEntries == null) {
            throw new ConfigValidationException("Expected action list at " + path + " in " + resourceName + ".");
        }
        return java.util.stream.IntStream.range(0, actionEntries.size())
                .mapToObj(index -> parseActionMap(actionEntries.get(index), path + "[" + index + "]", resourceName))
                .toList();
    }

    private static ActionDefinition parseActionMap(Object actionEntry, String path, String resourceName) {
        if (!(actionEntry instanceof Map<?, ?> actionMap)) {
            throw new ConfigValidationException("Expected action map at " + path + " in " + resourceName + ".");
        }
        return parseAction(actionMap, path, resourceName);
    }

    private static ActionDefinition parseAction(Map<?, ?> actionMap, String path, String resourceName) {
        Map<String, Object> values = new LinkedHashMap<>();
        String type = null;
        for (Map.Entry<?, ?> entry : actionMap.entrySet()) {
            if (entry.getKey() == null) {
                throw new ConfigValidationException("Null action key at " + path + " in " + resourceName + ".");
            }
            if (entry.getValue() == null) {
                throw new ConfigValidationException("Null action value at " + path + "." + entry.getKey() + " in " + resourceName + ".");
            }
            String key = String.valueOf(entry.getKey());
            if ("type".equals(key)) {
                type = String.valueOf(entry.getValue());
            } else {
                values.put(key, entry.getValue());
            }
        }
        if (type == null || type.isBlank()) {
            throw new ConfigValidationException("Missing required action type at " + path + " in " + resourceName + ".");
        }
        return new ActionDefinition(type, values);
    }

    private static ConfigurationSection requiredChildSection(
            ConfigurationSection parent,
            String key,
            String path,
            String resourceName) {
        ConfigurationSection section = parent.getConfigurationSection(key);
        if (section == null) {
            throw new ConfigValidationException("Expected configuration section at " + path + " in " + resourceName + ".");
        }
        return section;
    }

    private static ConfigurationSection optionalChildSection(
            ConfigurationSection parent,
            String key,
            String path,
            String resourceName) {
        if (!parent.contains(key)) {
            return null;
        }
        ConfigurationSection section = parent.getConfigurationSection(key);
        if (section == null) {
            throw new ConfigValidationException("Expected configuration section at " + path + " in " + resourceName + ".");
        }
        return section;
    }

    private static EntityType resolveEntityType(String key, String path, String resourceName) {
        try {
            return EntityType.valueOf(key);
        } catch (IllegalArgumentException exception) {
            throw new ConfigValidationException("Unknown entity type '" + key + "' at " + path + " in " + resourceName + ".");
        }
    }
}
