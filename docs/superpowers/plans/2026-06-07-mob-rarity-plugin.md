# Mob Rarity Plugin Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first working Paper/Java 25 mob rarity plugin with config-driven tiers, variants, spawn assignment, triggers, visuals, leveling, and optional integration adapters.

**Architecture:** Create a single Gradle Java plugin module with focused services for config, rarity selection, tagging, leveling, triggers, effects, integrations, visuals, and commands. Keep Bukkit event listeners thin: they translate Paper events into domain trigger requests, then call services that are unit tested without a live server.

**Tech Stack:** Java 25, Gradle Kotlin DSL, Paper API 26.1.2, Adventure MiniMessage, JUnit 5, AssertJ, Mockito, MockBukkit or Paper-compatible test doubles where useful, Shadow plugin.

---

## File Structure

- Create `settings.gradle.kts`: Gradle repository setup and root project name.
- Create `build.gradle.kts`: Java 25 toolchain, dependencies, tests, Shadow jar, resource expansion.
- Create `gradle/wrapper/gradle-wrapper.properties`, `gradlew`, `gradlew.bat`: wrapper files copied from the LandClaims project so the build is reproducible.
- Create `src/main/resources/plugin.yml`: Paper plugin metadata, soft dependencies, commands, permissions.
- Create `src/main/resources/config.yml`: commented default runtime settings.
- Create `src/main/resources/tiers.yml`: commented global rarity tiers and effects.
- Create `src/main/resources/mobs.yml`: commented per-mob overrides and example variants.
- Create `src/main/java/com/nick/mobrarity/RarityMobPlugin.java`: lifecycle and service wiring.
- Create `src/main/java/com/nick/mobrarity/config/*`: config loading, validation, and immutable config records.
- Create `src/main/java/com/nick/mobrarity/rarity/*`: tiers, variants, registry, weighted selection.
- Create `src/main/java/com/nick/mobrarity/level/*`: level calculation from distance and depth.
- Create `src/main/java/com/nick/mobrarity/tag/*`: persistent mob metadata.
- Create `src/main/java/com/nick/mobrarity/effect/*`: trigger context, conditions, actions, engine.
- Create `src/main/java/com/nick/mobrarity/integration/*`: LandClaims, VaultUnlocked, PlaceholderAPI adapters.
- Create `src/main/java/com/nick/mobrarity/visual/*`: MiniMessage formatting, nametag/title/particle services.
- Create `src/main/java/com/nick/mobrarity/listener/*`: spawn, damage, death, shear, tame, breed, interact listeners.
- Create `src/main/java/com/nick/mobrarity/command/*`: `/mobrarity` admin command executor and tab completer.
- Create `src/test/java/com/nick/mobrarity/**/*Test.java`: focused unit tests per service.

## Task 1: Project Foundation

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle/wrapper/gradle-wrapper.properties`
- Create: `gradlew`
- Create: `gradlew.bat`
- Create: `src/main/resources/plugin.yml`
- Create: `src/main/java/com/nick/mobrarity/RarityMobPlugin.java`
- Create: `src/test/java/com/nick/mobrarity/RarityMobPluginTest.java`

- [ ] **Step 1: Copy Gradle wrapper files**

Copy these files from `C:\Users\ncobu\OneDrive\Desktop\Scheduler Project\codex\landclaims` into this repo:

```text
gradlew
gradlew.bat
gradle/wrapper/gradle-wrapper.jar
gradle/wrapper/gradle-wrapper.properties
```

- [ ] **Step 2: Add Gradle settings**

Write `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.codemc.io/repository/maven-public/")
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    }
}

rootProject.name = "mob-rarity"
```

- [ ] **Step 3: Add Gradle build**

Write `build.gradle.kts`:

```kotlin
plugins {
    `java-library`
    id("com.gradleup.shadow") version "8.3.1"
}

group = "com.nick"
version = "1.0.0-SNAPSHOT"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.69-stable")
    compileOnly(files("libs/landclaims-api.jar"))
    compileOnly("net.milkbowl.vault:VaultAPI:1.7")
    compileOnly("me.clip:placeholderapi:2.11.6")

    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("org.mockito:mockito-core:5.18.0")
    testCompileOnly("io.papermc.paper:paper-api:26.1.2.build.69-stable")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.jar {
    enabled = false
}

tasks.shadowJar {
    archiveBaseName.set("MobRarity")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
```

- [ ] **Step 4: Add plugin metadata**

Write `src/main/resources/plugin.yml`:

```yaml
name: MobRarity
version: '${version}'
main: com.nick.mobrarity.RarityMobPlugin
api-version: '26.1.2'
author: Nick
description: Configurable mob rarities, variants, effects, and levels.
softdepend:
  - LandClaims
  - VaultUnlocked
  - PlaceholderAPI
commands:
  mobrarity:
    description: Admin commands for MobRarity.
    usage: /mobrarity
    aliases: [mr, mobr]
permissions:
  mobrarity.admin:
    description: Grants all MobRarity admin permissions.
    default: op
    children:
      mobrarity.reload: true
      mobrarity.inspect: true
      mobrarity.set: true
      mobrarity.spawn: true
      mobrarity.clear: true
      mobrarity.bypass.claim-check: true
  mobrarity.reload:
    default: op
  mobrarity.inspect:
    default: op
  mobrarity.set:
    default: op
  mobrarity.spawn:
    default: op
  mobrarity.clear:
    default: op
  mobrarity.bypass.claim-check:
    default: op
```

- [ ] **Step 5: Add minimal plugin class**

Write `src/main/java/com/nick/mobrarity/RarityMobPlugin.java`:

```java
package com.nick.mobrarity;

import org.bukkit.plugin.java.JavaPlugin;

public final class RarityMobPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResourceIfMissing("tiers.yml");
        saveResourceIfMissing("mobs.yml");
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
}
```

- [ ] **Step 6: Add smoke test**

Write `src/test/java/com/nick/mobrarity/RarityMobPluginTest.java`:

```java
package com.nick.mobrarity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class RarityMobPluginTest {
    @Test
    void pluginClassExists() {
        assertThat(RarityMobPlugin.class.getName()).isEqualTo("com.nick.mobrarity.RarityMobPlugin");
    }
}
```

- [ ] **Step 7: Run tests**

Run: `.\gradlew.bat test`

Expected: build succeeds with one passing test.

- [ ] **Step 8: Commit**

```bash
git add settings.gradle.kts build.gradle.kts gradlew gradlew.bat gradle src/main/resources/plugin.yml src/main/java/com/nick/mobrarity/RarityMobPlugin.java src/test/java/com/nick/mobrarity/RarityMobPluginTest.java
git commit -m "feat: scaffold mob rarity plugin"
```

## Task 2: Commented Default Configuration

**Files:**
- Create: `src/main/resources/config.yml`
- Create: `src/main/resources/tiers.yml`
- Create: `src/main/resources/mobs.yml`
- Create: `src/test/java/com/nick/mobrarity/config/DefaultConfigResourceTest.java`

- [ ] **Step 1: Write failing resource test**

```java
package com.nick.mobrarity.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

final class DefaultConfigResourceTest {
    @Test
    void defaultYamlFilesExistAndAreCommented() throws Exception {
        assertCommented("config.yml", "leveling:");
        assertCommented("tiers.yml", "tiers:");
        assertCommented("mobs.yml", "mobs:");
    }

    private static void assertCommented(String resourceName, String requiredSection) throws Exception {
        try (InputStream stream = DefaultConfigResourceTest.class.getClassLoader().getResourceAsStream(resourceName)) {
            assertThat(stream).as(resourceName).isNotNull();
            String text = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(text).contains(requiredSection);
            assertThat(text.lines().filter(line -> line.stripLeading().startsWith("#")).count())
                    .as(resourceName + " comment count")
                    .isGreaterThanOrEqualTo(8);
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat test --tests com.nick.mobrarity.config.DefaultConfigResourceTest`

Expected: FAIL because the YAML resources do not exist yet.

- [ ] **Step 3: Add commented `config.yml`**

```yaml
# MobRarity main settings.
# This file controls integrations, display behavior, leveling, and safety defaults.

integrations:
  landclaims:
    # allow: run effects even when LandClaims is missing.
    # deny-protected-effects: block claim-gated gameplay effects, allow visuals.
    # deny-all-effects: block all non-admin rarity triggers until LandClaims is available.
    missing-service-policy: allow
  vaultunlocked:
    enabled: true
  placeholderapi:
    enabled: true

display:
  # disabled, always, within-distance, or targeted.
  nametag-mode: targeted
  title-mode: targeted
  visibility-distance: 18.0
  target-distance: 32.0

leveling:
  enabled: true
  worlds:
    world:
      # world-spawn uses the world's current spawn location.
      origin: world-spawn
      blocks-per-level: 250
      min-level: 1
      max-level: 100
      depth-bonus:
        enabled: true
        starts-below-y: 64
        levels-per-y: 0.15
        max-bonus-levels: 25
```

- [ ] **Step 4: Add commented `tiers.yml`**

```yaml
# Global rarity tiers.
# Tiers define shared display, base spawn weights, scaling, and effects.

tiers:
  common:
    weight: 100
    display-name: "<gray>Common</gray>"
  rare:
    weight: 8
    display-name: "<aqua>Rare</aqua>"
    rarity-weight-modifier:
      per-level: 0.03
    display:
      nametag: "<aqua>[Lv %mobrarity_level%] %mobrarity_variant%</aqua>"
    visuals:
      particles:
        enabled: true
        particle: HAPPY_VILLAGER
        interval-ticks: 40
        count: 3
```

- [ ] **Step 5: Add commented `mobs.yml`**

```yaml
# Per-mob rarity settings.
# Entity keys use Bukkit EntityType names such as ZOMBIE, SHEEP, CREEPER, or SKELETON.

mobs:
  SHEEP:
    spawn-sources:
      # NATURAL covers ordinary world spawning.
      NATURAL: true
      # SPAWNER covers mob spawner blocks.
      SPAWNER: false
      # SPAWN_EGG covers player-used spawn eggs.
      SPAWN_EGG: true
      # BREEDING covers child mobs created through breeding.
      BREEDING: true
      # COMMAND covers this plugin's admin spawn command.
      COMMAND: true
      # PLUGIN covers other plugin-created entities when Paper reports plugin spawning.
      PLUGIN: true
    variants:
      toxic_sheep:
        tier: rare
        weight: 2
        display:
          nametag: "<green>[Lv %mobrarity_level%] Toxic Sheep</green>"
        triggers:
          on_aura_tick:
            interval-ticks: 40
            radius: 4.0
            actions:
              - type: potion_effect
                effect: POISON
                duration-ticks: 80
                amplifier: 0
      rare_sheep:
        tier: rare
        weight: 3
        display:
          nametag: "<aqua>[Lv %mobrarity_level%] Rare Sheep</aqua>"
        triggers:
          on_shear:
            chance: 0.25
            actions:
              - type: item_drop
                material: DIAMOND
                amount: 1
              - type: item_drop
                material: GOLD_NUGGET
                amount: "2-8"
```

- [ ] **Step 6: Run test**

Run: `.\gradlew.bat test --tests com.nick.mobrarity.config.DefaultConfigResourceTest`

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add src/main/resources/config.yml src/main/resources/tiers.yml src/main/resources/mobs.yml src/test/java/com/nick/mobrarity/config/DefaultConfigResourceTest.java
git commit -m "feat: add commented default configuration"
```

## Task 3: Config Domain And Validation

**Files:**
- Create: `src/main/java/com/nick/mobrarity/config/ConfigValidationException.java`
- Create: `src/main/java/com/nick/mobrarity/config/ValidationResult.java`
- Create: `src/main/java/com/nick/mobrarity/config/ConfigService.java`
- Create: `src/main/java/com/nick/mobrarity/rarity/RarityTier.java`
- Create: `src/main/java/com/nick/mobrarity/rarity/MobVariant.java`
- Create: `src/main/java/com/nick/mobrarity/rarity/MobProfile.java`
- Create: `src/main/java/com/nick/mobrarity/effect/TriggerDefinition.java`
- Create: `src/main/java/com/nick/mobrarity/effect/ActionDefinition.java`
- Create: `src/test/java/com/nick/mobrarity/config/ConfigServiceTest.java`

- [ ] **Step 1: Write config tests**

```java
package com.nick.mobrarity.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.nick.mobrarity.rarity.MobProfile;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;

final class ConfigServiceTest {
    @Test
    void loadsDefaultSheepVariants() {
        ConfigService service = ConfigService.fromResources();

        MobProfile sheep = service.snapshot().mobProfiles().get(EntityType.SHEEP);

        assertThat(sheep).isNotNull();
        assertThat(sheep.variants()).containsKeys("toxic_sheep", "rare_sheep");
        assertThat(service.snapshot().tiers()).containsKeys("common", "rare");
    }

    @Test
    void reportsUnknownTierReference() {
        ValidationResult result = ConfigService.validateVariantTier("missing", java.util.Set.of("rare"));

        assertThat(result.valid()).isFalse();
        assertThat(result.messages()).contains("Unknown tier 'missing'.");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat test --tests com.nick.mobrarity.config.ConfigServiceTest`

Expected: FAIL because config classes do not exist.

- [ ] **Step 3: Add validation result classes**

```java
package com.nick.mobrarity.config;

import java.util.List;

public record ValidationResult(boolean valid, List<String> messages) {
    public static ValidationResult ok() {
        return new ValidationResult(true, List.of());
    }

    public static ValidationResult error(String message) {
        return new ValidationResult(false, List.of(message));
    }
}
```

```java
package com.nick.mobrarity.config;

public final class ConfigValidationException extends RuntimeException {
    public ConfigValidationException(String message) {
        super(message);
    }
}
```

- [ ] **Step 4: Add domain records**

```java
package com.nick.mobrarity.rarity;

import com.nick.mobrarity.effect.TriggerDefinition;
import java.util.Map;

public record RarityTier(String key, double weight, String displayName, Map<String, TriggerDefinition> triggers) {
}
```

```java
package com.nick.mobrarity.rarity;

import com.nick.mobrarity.effect.TriggerDefinition;
import java.util.Map;

public record MobVariant(String key, String tierKey, double weight, String nametag, Map<String, TriggerDefinition> triggers) {
}
```

```java
package com.nick.mobrarity.rarity;

import java.util.Map;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

public record MobProfile(Map<SpawnReason, Boolean> spawnSources, Map<String, MobVariant> variants) {
    public boolean allows(SpawnReason reason) {
        return spawnSources.getOrDefault(reason, false);
    }
}
```

```java
package com.nick.mobrarity.effect;

import java.util.List;

public record TriggerDefinition(String key, double chance, long intervalTicks, double radius, List<ActionDefinition> actions) {
}
```

```java
package com.nick.mobrarity.effect;

import java.util.Map;

public record ActionDefinition(String type, Map<String, Object> values) {
}
```

- [ ] **Step 5: Add config service**

```java
package com.nick.mobrarity.config;

import com.nick.mobrarity.effect.ActionDefinition;
import com.nick.mobrarity.effect.TriggerDefinition;
import com.nick.mobrarity.rarity.MobProfile;
import com.nick.mobrarity.rarity.MobVariant;
import com.nick.mobrarity.rarity.RarityTier;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
        ClassLoader loader = ConfigService.class.getClassLoader();
        YamlConfiguration tiersYaml = YamlConfiguration.loadConfiguration(new InputStreamReader(loader.getResourceAsStream("tiers.yml"), StandardCharsets.UTF_8));
        YamlConfiguration mobsYaml = YamlConfiguration.loadConfiguration(new InputStreamReader(loader.getResourceAsStream("mobs.yml"), StandardCharsets.UTF_8));
        return new ConfigService(new ConfigSnapshot(loadTiers(tiersYaml), loadMobs(mobsYaml)));
    }

    public ConfigSnapshot snapshot() {
        return snapshot;
    }

    public static ValidationResult validateVariantTier(String tierKey, Set<String> knownTiers) {
        return knownTiers.contains(tierKey) ? ValidationResult.ok() : ValidationResult.error("Unknown tier '" + tierKey + "'.");
    }

    private static Map<String, RarityTier> loadTiers(YamlConfiguration yaml) {
        Map<String, RarityTier> tiers = new LinkedHashMap<>();
        ConfigurationSection section = yaml.getConfigurationSection("tiers");
        for (String key : section.getKeys(false)) {
            ConfigurationSection tier = section.getConfigurationSection(key);
            tiers.put(key, new RarityTier(key, tier.getDouble("weight"), tier.getString("display-name", key), Map.of()));
        }
        return Map.copyOf(tiers);
    }

    private static Map<EntityType, MobProfile> loadMobs(YamlConfiguration yaml) {
        Map<EntityType, MobProfile> profiles = new LinkedHashMap<>();
        ConfigurationSection mobs = yaml.getConfigurationSection("mobs");
        for (String typeKey : mobs.getKeys(false)) {
            EntityType entityType = EntityType.valueOf(typeKey);
            ConfigurationSection mob = mobs.getConfigurationSection(typeKey);
            profiles.put(entityType, new MobProfile(loadSpawnSources(mob.getConfigurationSection("spawn-sources")), loadVariants(mob.getConfigurationSection("variants"))));
        }
        return Map.copyOf(profiles);
    }

    private static Map<SpawnReason, Boolean> loadSpawnSources(ConfigurationSection section) {
        Map<SpawnReason, Boolean> sources = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            sources.put(SpawnReason.valueOf(key), section.getBoolean(key));
        }
        return Map.copyOf(sources);
    }

    private static Map<String, MobVariant> loadVariants(ConfigurationSection section) {
        Map<String, MobVariant> variants = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection variant = section.getConfigurationSection(key);
            variants.put(key, new MobVariant(key, variant.getString("tier"), variant.getDouble("weight"), variant.getString("display.nametag", key), Map.of()));
        }
        return Map.copyOf(variants);
    }

    public record ConfigSnapshot(Map<String, RarityTier> tiers, Map<EntityType, MobProfile> mobProfiles) {
    }
}
```

- [ ] **Step 6: Run config tests**

Run: `.\gradlew.bat test --tests com.nick.mobrarity.config.ConfigServiceTest`

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/nick/mobrarity/config src/main/java/com/nick/mobrarity/rarity src/main/java/com/nick/mobrarity/effect src/test/java/com/nick/mobrarity/config/ConfigServiceTest.java
git commit -m "feat: load rarity configuration"
```

## Task 4: Leveling And Weighted Selection

**Files:**
- Create: `src/main/java/com/nick/mobrarity/level/LevelSettings.java`
- Create: `src/main/java/com/nick/mobrarity/level/MobLevelService.java`
- Create: `src/main/java/com/nick/mobrarity/rarity/WeightedSelector.java`
- Create: `src/main/java/com/nick/mobrarity/rarity/SpawnRarityService.java`
- Create: `src/test/java/com/nick/mobrarity/level/MobLevelServiceTest.java`
- Create: `src/test/java/com/nick/mobrarity/rarity/WeightedSelectorTest.java`
- Create: `src/test/java/com/nick/mobrarity/rarity/SpawnRarityServiceTest.java`

- [ ] **Step 1: Write leveling tests**

```java
package com.nick.mobrarity.level;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class MobLevelServiceTest {
    @Test
    void calculatesHorizontalLevel() {
        LevelSettings settings = new LevelSettings(true, 250, 1, 100, true, 64, 0.15, 25);
        MobLevelService service = new MobLevelService(settings);

        assertThat(service.calculateLevel(750, 70)).isEqualTo(4);
    }

    @Test
    void addsDepthBonusBelowThreshold() {
        LevelSettings settings = new LevelSettings(true, 250, 1, 100, true, 64, 0.15, 25);
        MobLevelService service = new MobLevelService(settings);

        assertThat(service.calculateLevel(0, 44)).isEqualTo(4);
    }
}
```

- [ ] **Step 2: Write weighted selector test**

```java
package com.nick.mobrarity.rarity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;

final class WeightedSelectorTest {
    @Test
    void selectsEntryByWeightRange() {
        WeightedSelector<String> selector = new WeightedSelector<>(RandomGenerator.of("L64X128MixRandom"));

        Optional<String> selected = selector.select(List.of(
                new WeightedSelector.Entry<>("common", 100),
                new WeightedSelector.Entry<>("rare", 10)
        ));

        assertThat(selected).isPresent();
    }

    @Test
    void ignoresZeroWeightEntries() {
        WeightedSelector<String> selector = new WeightedSelector<>(RandomGenerator.of("L64X128MixRandom"));

        assertThat(selector.select(List.of(new WeightedSelector.Entry<>("disabled", 0)))).isEmpty();
    }
}
```

- [ ] **Step 3: Run tests to verify they fail**

Also write `src/test/java/com/nick/mobrarity/rarity/SpawnRarityServiceTest.java`:

```java
package com.nick.mobrarity.rarity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Optional;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.junit.jupiter.api.Test;

final class SpawnRarityServiceTest {
    @Test
    void selectsVariantWhenSpawnReasonAllowed() {
        MobVariant variant = new MobVariant("rare_sheep", "rare", 3, "<aqua>Rare Sheep</aqua>", Map.of());
        MobProfile profile = new MobProfile(Map.of(SpawnReason.NATURAL, true), Map.of("rare_sheep", variant));
        SpawnRarityService service = new SpawnRarityService(entries -> Optional.of(variant));

        Optional<MobVariant> selected = service.selectVariant(profile, SpawnReason.NATURAL);

        assertThat(selected).contains(variant);
    }

    @Test
    void skipsVariantWhenSpawnReasonDenied() {
        MobVariant variant = new MobVariant("rare_sheep", "rare", 3, "<aqua>Rare Sheep</aqua>", Map.of());
        MobProfile profile = new MobProfile(Map.of(SpawnReason.SPAWNER, false), Map.of("rare_sheep", variant));
        SpawnRarityService service = new SpawnRarityService(entries -> Optional.of(variant));

        assertThat(service.selectVariant(profile, SpawnReason.SPAWNER)).isEmpty();
    }
}
```

Run: `.\gradlew.bat test --tests com.nick.mobrarity.level.MobLevelServiceTest --tests com.nick.mobrarity.rarity.WeightedSelectorTest --tests com.nick.mobrarity.rarity.SpawnRarityServiceTest`

Expected: FAIL because leveling and selector classes do not exist.

- [ ] **Step 4: Implement leveling**

```java
package com.nick.mobrarity.level;

public record LevelSettings(
        boolean enabled,
        int blocksPerLevel,
        int minLevel,
        int maxLevel,
        boolean depthBonusEnabled,
        int startsBelowY,
        double levelsPerY,
        int maxBonusLevels
) {
}
```

```java
package com.nick.mobrarity.level;

public final class MobLevelService {
    private final LevelSettings settings;

    public MobLevelService(LevelSettings settings) {
        this.settings = settings;
    }

    public int calculateLevel(double horizontalDistance, double y) {
        if (!settings.enabled()) {
            return settings.minLevel();
        }
        int horizontal = settings.minLevel() + (int) Math.floor(horizontalDistance / settings.blocksPerLevel());
        int bonus = 0;
        if (settings.depthBonusEnabled() && y < settings.startsBelowY()) {
            bonus = Math.min(settings.maxBonusLevels(), (int) Math.floor((settings.startsBelowY() - y) * settings.levelsPerY()));
        }
        return Math.max(settings.minLevel(), Math.min(settings.maxLevel(), horizontal + bonus));
    }
}
```

- [ ] **Step 5: Implement weighted selector**

```java
package com.nick.mobrarity.rarity;

import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;

public final class WeightedSelector<T> {
    private final RandomGenerator random;

    public WeightedSelector(RandomGenerator random) {
        this.random = random;
    }

    public Optional<T> select(List<Entry<T>> entries) {
        double total = entries.stream().mapToDouble(Entry::weight).filter(weight -> weight > 0).sum();
        if (total <= 0) {
            return Optional.empty();
        }
        double cursor = random.nextDouble(total);
        double running = 0;
        for (Entry<T> entry : entries) {
            if (entry.weight() <= 0) {
                continue;
            }
            running += entry.weight();
            if (cursor < running) {
                return Optional.of(entry.value());
            }
        }
        return Optional.empty();
    }

    public record Entry<T>(T value, double weight) {
    }
}
```

- [ ] **Step 6: Implement spawn rarity service**

```java
package com.nick.mobrarity.rarity;

import java.util.List;
import java.util.Optional;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

public final class SpawnRarityService {
    private final VariantSelector selector;

    public SpawnRarityService(VariantSelector selector) {
        this.selector = selector;
    }

    public Optional<MobVariant> selectVariant(MobProfile profile, SpawnReason reason) {
        if (!profile.allows(reason)) {
            return Optional.empty();
        }
        List<WeightedSelector.Entry<MobVariant>> entries = profile.variants().values().stream()
                .map(variant -> new WeightedSelector.Entry<>(variant, variant.weight()))
                .toList();
        return selector.select(entries);
    }

    @FunctionalInterface
    public interface VariantSelector {
        Optional<MobVariant> select(List<WeightedSelector.Entry<MobVariant>> entries);
    }
}
```

- [ ] **Step 7: Run tests**

Run: `.\gradlew.bat test --tests com.nick.mobrarity.level.MobLevelServiceTest --tests com.nick.mobrarity.rarity.WeightedSelectorTest --tests com.nick.mobrarity.rarity.SpawnRarityServiceTest`

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/nick/mobrarity/level src/main/java/com/nick/mobrarity/rarity/WeightedSelector.java src/main/java/com/nick/mobrarity/rarity/SpawnRarityService.java src/test/java/com/nick/mobrarity/level src/test/java/com/nick/mobrarity/rarity
git commit -m "feat: add mob leveling and weighted selection"
```

## Task 5: Mob Tagging And Spawn Assignment

**Files:**
- Create: `src/main/java/com/nick/mobrarity/tag/MobRarityData.java`
- Create: `src/main/java/com/nick/mobrarity/tag/MobTagService.java`
- Create: `src/main/java/com/nick/mobrarity/listener/MobSpawnListener.java`
- Modify: `src/main/java/com/nick/mobrarity/RarityMobPlugin.java`
- Create: `src/test/java/com/nick/mobrarity/tag/MobRarityDataTest.java`

- [ ] **Step 1: Write metadata serialization test**

```java
package com.nick.mobrarity.tag;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class MobRarityDataTest {
    @Test
    void serializesAndParsesData() {
        MobRarityData data = new MobRarityData("rare", "rare_sheep", 14);

        assertThat(MobRarityData.parse(data.serialize())).contains(data);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat test --tests com.nick.mobrarity.tag.MobRarityDataTest`

Expected: FAIL because tag classes do not exist.

- [ ] **Step 3: Implement metadata record**

```java
package com.nick.mobrarity.tag;

import java.util.Optional;

public record MobRarityData(String tierKey, String variantKey, int level) {
    public String serialize() {
        return tierKey + "|" + variantKey + "|" + level;
    }

    public static Optional<MobRarityData> parse(String value) {
        String[] parts = value.split("\\|", -1);
        if (parts.length != 3) {
            return Optional.empty();
        }
        try {
            return Optional.of(new MobRarityData(parts[0], parts[1], Integer.parseInt(parts[2])));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }
}
```

- [ ] **Step 4: Implement Bukkit tag service**

```java
package com.nick.mobrarity.tag;

import java.util.Optional;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class MobTagService {
    private final NamespacedKey key;

    public MobTagService(Plugin plugin) {
        this.key = new NamespacedKey(plugin, "rarity_data");
    }

    public void tag(LivingEntity entity, MobRarityData data) {
        entity.getPersistentDataContainer().set(key, PersistentDataType.STRING, data.serialize());
    }

    public Optional<MobRarityData> read(LivingEntity entity) {
        String value = entity.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        return value == null ? Optional.empty() : MobRarityData.parse(value);
    }

    public void clear(LivingEntity entity) {
        entity.getPersistentDataContainer().remove(key);
    }
}
```

- [ ] **Step 5: Add spawn assignment listener**

```java
package com.nick.mobrarity.listener;

import com.nick.mobrarity.config.ConfigService;
import com.nick.mobrarity.level.MobLevelService;
import com.nick.mobrarity.rarity.MobProfile;
import com.nick.mobrarity.rarity.MobVariant;
import com.nick.mobrarity.rarity.SpawnRarityService;
import com.nick.mobrarity.tag.MobRarityData;
import com.nick.mobrarity.tag.MobTagService;
import java.util.Optional;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

public final class MobSpawnListener implements Listener {
    private final ConfigService configService;
    private final SpawnRarityService spawnRarityService;
    private final MobLevelService mobLevelService;
    private final MobTagService mobTagService;

    public MobSpawnListener(
            ConfigService configService,
            SpawnRarityService spawnRarityService,
            MobLevelService mobLevelService,
            MobTagService mobTagService
    ) {
        this.configService = configService;
        this.spawnRarityService = spawnRarityService;
        this.mobLevelService = mobLevelService;
        this.mobTagService = mobTagService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        MobProfile profile = configService.snapshot().mobProfiles().get(event.getEntityType());
        if (profile == null) {
            return;
        }
        Optional<MobVariant> selected = spawnRarityService.selectVariant(profile, event.getSpawnReason());
        if (selected.isEmpty()) {
            return;
        }
        int level = mobLevelService.calculateLevel(0, event.getLocation().getY());
        MobVariant variant = selected.get();
        mobTagService.tag(event.getEntity(), new MobRarityData(variant.tierKey(), variant.key(), level));
    }
}
```

- [ ] **Step 6: Wire listener in plugin**

Update `RarityMobPlugin.onEnable()`:

```java
getServer().getPluginManager().registerEvents(new MobSpawnListener(configService, spawnRarityService, mobLevelService, mobTagService), this);
```

- [ ] **Step 7: Run tests**

Run: `.\gradlew.bat test --tests com.nick.mobrarity.tag.MobRarityDataTest`

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/nick/mobrarity/tag src/main/java/com/nick/mobrarity/listener/MobSpawnListener.java src/main/java/com/nick/mobrarity/RarityMobPlugin.java src/test/java/com/nick/mobrarity/tag
git commit -m "feat: add mob rarity tagging"
```

## Task 6: Protection, Economy, And Placeholder Adapters

**Files:**
- Create: `src/main/java/com/nick/mobrarity/integration/ProtectionAdapter.java`
- Create: `src/main/java/com/nick/mobrarity/integration/LandClaimsProtectionAdapter.java`
- Create: `src/main/java/com/nick/mobrarity/integration/ProtectionFallbackPolicy.java`
- Create: `src/main/java/com/nick/mobrarity/integration/EconomyAdapter.java`
- Create: `src/main/java/com/nick/mobrarity/integration/NoEconomyAdapter.java`
- Create: `src/main/java/com/nick/mobrarity/integration/VaultUnlockedEconomyAdapter.java`
- Create: `src/main/java/com/nick/mobrarity/integration/TextPlaceholderService.java`
- Create: `src/test/java/com/nick/mobrarity/integration/ProtectionFallbackPolicyTest.java`
- Create: `src/test/java/com/nick/mobrarity/integration/TextPlaceholderServiceTest.java`

- [ ] **Step 1: Write fallback policy test**

```java
package com.nick.mobrarity.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class ProtectionFallbackPolicyTest {
    @Test
    void denyProtectedEffectsStillAllowsVisuals() {
        assertThat(ProtectionFallbackPolicy.DENY_PROTECTED_EFFECTS.allowsMissingService("message")).isTrue();
        assertThat(ProtectionFallbackPolicy.DENY_PROTECTED_EFFECTS.allowsMissingService("item_drop")).isFalse();
    }
}
```

- [ ] **Step 2: Write text replacement test**

```java
package com.nick.mobrarity.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

final class TextPlaceholderServiceTest {
    @Test
    void replacesBuiltInValues() {
        TextPlaceholderService service = new TextPlaceholderService(false);

        String text = service.replaceBuiltIns("<aqua>[Lv %mobrarity_level%] %mobrarity_variant%</aqua>", Map.of(
                "mobrarity_level", "12",
                "mobrarity_variant", "Rare Sheep"
        ));

        assertThat(text).isEqualTo("<aqua>[Lv 12] Rare Sheep</aqua>");
    }
}
```

- [ ] **Step 3: Run tests to verify they fail**

Run: `.\gradlew.bat test --tests com.nick.mobrarity.integration.ProtectionFallbackPolicyTest --tests com.nick.mobrarity.integration.TextPlaceholderServiceTest`

Expected: FAIL because integration classes do not exist.

- [ ] **Step 4: Add protection policy**

```java
package com.nick.mobrarity.integration;

import java.util.Set;

public enum ProtectionFallbackPolicy {
    ALLOW,
    DENY_PROTECTED_EFFECTS,
    DENY_ALL_EFFECTS;

    private static final Set<String> VISUAL_ACTIONS = Set.of("message", "particle", "sound");

    public boolean allowsMissingService(String actionType) {
        return switch (this) {
            case ALLOW -> true;
            case DENY_PROTECTED_EFFECTS -> VISUAL_ACTIONS.contains(actionType);
            case DENY_ALL_EFFECTS -> false;
        };
    }
}
```

- [ ] **Step 5: Add adapter interfaces**

```java
package com.nick.mobrarity.integration;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface ProtectionAdapter {
    boolean canRun(Player player, Location location, String actionKey, String actionType);
}
```

```java
package com.nick.mobrarity.integration;

import org.bukkit.entity.Player;

public interface EconomyAdapter {
    boolean available();

    void deposit(Player player, double amount);
}
```

- [ ] **Step 6: Add no-economy adapter**

```java
package com.nick.mobrarity.integration;

import org.bukkit.entity.Player;

public final class NoEconomyAdapter implements EconomyAdapter {
    @Override
    public boolean available() {
        return false;
    }

    @Override
    public void deposit(Player player, double amount) {
    }
}
```

- [ ] **Step 7: Add text service**

```java
package com.nick.mobrarity.integration;

import java.util.Map;

public final class TextPlaceholderService {
    private final boolean placeholderApiAvailable;

    public TextPlaceholderService(boolean placeholderApiAvailable) {
        this.placeholderApiAvailable = placeholderApiAvailable;
    }

    public String replaceBuiltIns(String input, Map<String, String> values) {
        String output = input;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            output = output.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return output;
    }

    public boolean placeholderApiAvailable() {
        return placeholderApiAvailable;
    }
}
```

- [ ] **Step 8: Add compile-only LandClaims adapter after API jar exists**

Build or copy `landclaims-api.jar` into `libs/landclaims-api.jar`, then add:

```java
package com.nick.mobrarity.integration;

import com.nick.landclaims.api.LandClaimsApi;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class LandClaimsProtectionAdapter implements ProtectionAdapter {
    private final LandClaimsApi api;
    private final ProtectionFallbackPolicy fallbackPolicy;

    public LandClaimsProtectionAdapter(LandClaimsApi api, ProtectionFallbackPolicy fallbackPolicy) {
        this.api = api;
        this.fallbackPolicy = fallbackPolicy;
    }

    @Override
    public boolean canRun(Player player, Location location, String actionKey, String actionType) {
        if (api == null) {
            return fallbackPolicy.allowsMissingService(actionType);
        }
        if (player.hasPermission("mobrarity.bypass.claim-check")) {
            return true;
        }
        return api.canInteract(player, location, actionKey);
    }
}
```

- [ ] **Step 9: Run tests**

Run: `.\gradlew.bat test --tests com.nick.mobrarity.integration.ProtectionFallbackPolicyTest --tests com.nick.mobrarity.integration.TextPlaceholderServiceTest`

Expected: PASS.

- [ ] **Step 10: Commit**

```bash
git add src/main/java/com/nick/mobrarity/integration src/test/java/com/nick/mobrarity/integration libs
git commit -m "feat: add optional integration adapters"
```

## Task 7: Effect Engine Core

**Files:**
- Create: `src/main/java/com/nick/mobrarity/effect/TriggerType.java`
- Create: `src/main/java/com/nick/mobrarity/effect/TriggerContext.java`
- Create: `src/main/java/com/nick/mobrarity/effect/EffectAction.java`
- Create: `src/main/java/com/nick/mobrarity/effect/EffectActionRegistry.java`
- Create: `src/main/java/com/nick/mobrarity/effect/EffectEngine.java`
- Create: `src/main/java/com/nick/mobrarity/effect/actions/ItemDropAction.java`
- Create: `src/main/java/com/nick/mobrarity/effect/actions/PotionEffectAction.java`
- Create: `src/main/java/com/nick/mobrarity/effect/actions/MessageAction.java`
- Create: `src/test/java/com/nick/mobrarity/effect/EffectEngineTest.java`

- [ ] **Step 1: Write engine test**

```java
package com.nick.mobrarity.effect;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class EffectEngineTest {
    @Test
    void skipsActionWhenChanceFails() {
        List<String> executed = new ArrayList<>();
        EffectEngine engine = new EffectEngine(type -> context -> executed.add(type), () -> 0.99);

        engine.run(new TriggerDefinition("on_shear", 0.25, 0, 0, List.of(new ActionDefinition("message", Map.of()))), TriggerContext.empty());

        assertThat(executed).isEmpty();
    }

    @Test
    void executesConfiguredActionsWhenChancePasses() {
        List<String> executed = new ArrayList<>();
        EffectEngine engine = new EffectEngine(type -> context -> executed.add(type), () -> 0.0);

        engine.run(new TriggerDefinition("on_shear", 0.25, 0, 0, List.of(new ActionDefinition("message", Map.of()))), TriggerContext.empty());

        assertThat(executed).containsExactly("message");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat test --tests com.nick.mobrarity.effect.EffectEngineTest`

Expected: FAIL because engine classes do not exist.

- [ ] **Step 3: Add core engine types**

```java
package com.nick.mobrarity.effect;

public enum TriggerType {
    ON_SPAWN,
    ON_DAMAGE,
    ON_DAMAGED_BY_MOB,
    ON_DEATH,
    ON_SHEAR,
    ON_TAME,
    ON_BREED,
    ON_INTERACT,
    ON_AURA_TICK,
    ON_TARGET
}
```

```java
package com.nick.mobrarity.effect;

public record TriggerContext() {
    public static TriggerContext empty() {
        return new TriggerContext();
    }
}
```

```java
package com.nick.mobrarity.effect;

@FunctionalInterface
public interface EffectAction {
    void execute(TriggerContext context);
}
```

```java
package com.nick.mobrarity.effect;

@FunctionalInterface
public interface EffectActionRegistry {
    EffectAction action(String type);
}
```

- [ ] **Step 4: Add effect engine**

```java
package com.nick.mobrarity.effect;

import java.util.function.DoubleSupplier;

public final class EffectEngine {
    private final EffectActionRegistry registry;
    private final DoubleSupplier random;

    public EffectEngine(EffectActionRegistry registry, DoubleSupplier random) {
        this.registry = registry;
        this.random = random;
    }

    public void run(TriggerDefinition trigger, TriggerContext context) {
        if (trigger.chance() > 0 && random.getAsDouble() > trigger.chance()) {
            return;
        }
        for (ActionDefinition action : trigger.actions()) {
            registry.action(action.type()).execute(context);
        }
    }
}
```

- [ ] **Step 5: Run tests**

Run: `.\gradlew.bat test --tests com.nick.mobrarity.effect.EffectEngineTest`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/nick/mobrarity/effect src/test/java/com/nick/mobrarity/effect/EffectEngineTest.java
git commit -m "feat: add effect engine core"
```

## Task 8: Event Routing

**Files:**
- Create: `src/main/java/com/nick/mobrarity/listener/PlayerDamageTracker.java`
- Create: `src/main/java/com/nick/mobrarity/listener/MobDamageListener.java`
- Create: `src/main/java/com/nick/mobrarity/listener/MobDeathListener.java`
- Create: `src/main/java/com/nick/mobrarity/listener/MobActivityListener.java`
- Modify: `src/main/java/com/nick/mobrarity/RarityMobPlugin.java`
- Create: `src/test/java/com/nick/mobrarity/listener/PlayerDamageTrackerTest.java`

- [ ] **Step 1: Write damage tracker test**

```java
package com.nick.mobrarity.listener;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

final class PlayerDamageTrackerTest {
    @Test
    void expiresOldDamage() {
        PlayerDamageTracker tracker = new PlayerDamageTracker(100);
        UUID entityId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();

        tracker.record(entityId, playerId, 50);

        assertThat(tracker.lastPlayer(entityId, 120)).contains(playerId);
        assertThat(tracker.lastPlayer(entityId, 151)).isEmpty();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat test --tests com.nick.mobrarity.listener.PlayerDamageTrackerTest`

Expected: FAIL because tracker does not exist.

- [ ] **Step 3: Add damage tracker**

```java
package com.nick.mobrarity.listener;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerDamageTracker {
    private final long expiryTicks;
    private final Map<UUID, Entry> entries = new ConcurrentHashMap<>();

    public PlayerDamageTracker(long expiryTicks) {
        this.expiryTicks = expiryTicks;
    }

    public void record(UUID entityId, UUID playerId, long currentTick) {
        entries.put(entityId, new Entry(playerId, currentTick));
    }

    public Optional<UUID> lastPlayer(UUID entityId, long currentTick) {
        Entry entry = entries.get(entityId);
        if (entry == null || currentTick - entry.tick() > expiryTicks) {
            return Optional.empty();
        }
        return Optional.of(entry.playerId());
    }

    private record Entry(UUID playerId, long tick) {
    }
}
```

- [ ] **Step 4: Add player-caused damage listener**

Create `src/main/java/com/nick/mobrarity/listener/MobDamageListener.java`:

```java
package com.nick.mobrarity.listener;

import java.util.Optional;
import java.util.function.LongSupplier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

public final class MobDamageListener implements Listener {
    private final PlayerDamageTracker tracker;
    private final LongSupplier currentTick;

    public MobDamageListener(PlayerDamageTracker tracker, LongSupplier currentTick) {
        this.tracker = tracker;
        this.currentTick = currentTick;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity livingEntity)) {
            return;
        }
        playerFromDamager(event).ifPresent(player -> tracker.record(livingEntity.getUniqueId(), player.getUniqueId(), currentTick.getAsLong()));
    }

    private Optional<Player> playerFromDamager(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            return Optional.of(player);
        }
        if (event.getDamager() instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) {
                return Optional.of(player);
            }
        }
        return Optional.empty();
    }
}
```

Create `src/main/java/com/nick/mobrarity/listener/MobDeathListener.java`:

```java
package com.nick.mobrarity.listener;

import com.nick.mobrarity.effect.EffectEngine;
import com.nick.mobrarity.effect.TriggerContext;
import com.nick.mobrarity.effect.TriggerDefinition;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.LongSupplier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public final class MobDeathListener implements Listener {
    private final PlayerDamageTracker tracker;
    private final EffectEngine effectEngine;
    private final Function<LivingEntity, Optional<TriggerDefinition>> deathTriggerLookup;
    private final LongSupplier currentTick;

    public MobDeathListener(
            PlayerDamageTracker tracker,
            EffectEngine effectEngine,
            Function<LivingEntity, Optional<TriggerDefinition>> deathTriggerLookup,
            LongSupplier currentTick
    ) {
        this.tracker = tracker;
        this.effectEngine = effectEngine;
        this.deathTriggerLookup = deathTriggerLookup;
        this.currentTick = currentTick;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        tracker.lastPlayer(event.getEntity().getUniqueId(), currentTick.getAsLong()).ifPresent(playerId -> {
            deathTriggerLookup.apply(event.getEntity())
                    .ifPresent(trigger -> effectEngine.run(trigger, TriggerContext.empty()));
        });
    }
}
```

Create `src/main/java/com/nick/mobrarity/listener/MobActivityListener.java`:

```java
package com.nick.mobrarity.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;

public final class MobActivityListener implements Listener {
    @EventHandler(ignoreCancelled = true)
    public void onShear(PlayerShearEntityEvent event) {
    }

    @EventHandler(ignoreCancelled = true)
    public void onTame(EntityTameEvent event) {
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreed(EntityBreedEvent event) {
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
    }
}
```

- [ ] **Step 5: Run tests**

Run: `.\gradlew.bat test --tests com.nick.mobrarity.listener.PlayerDamageTrackerTest`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/nick/mobrarity/listener src/main/java/com/nick/mobrarity/RarityMobPlugin.java src/test/java/com/nick/mobrarity/listener
git commit -m "feat: route mob rarity events"
```

## Task 9: Visuals And Commands

**Files:**
- Create: `src/main/java/com/nick/mobrarity/visual/DisplayMode.java`
- Create: `src/main/java/com/nick/mobrarity/visual/MobTextFormatter.java`
- Create: `src/main/java/com/nick/mobrarity/visual/VisualService.java`
- Create: `src/main/java/com/nick/mobrarity/command/MobRarityCommand.java`
- Create: `src/main/java/com/nick/mobrarity/command/MobRarityTabCompleter.java`
- Modify: `src/main/java/com/nick/mobrarity/RarityMobPlugin.java`
- Create: `src/test/java/com/nick/mobrarity/visual/MobTextFormatterTest.java`

- [ ] **Step 1: Write formatter test**

```java
package com.nick.mobrarity.visual;

import static org.assertj.core.api.Assertions.assertThat;

import com.nick.mobrarity.integration.TextPlaceholderService;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class MobTextFormatterTest {
    @Test
    void formatsBuiltInValuesBeforeMiniMessage() {
        MobTextFormatter formatter = new MobTextFormatter(new TextPlaceholderService(false));

        String formatted = formatter.formatPlain("[Lv %mobrarity_level%] %mobrarity_variant%", Map.of(
                "mobrarity_level", "7",
                "mobrarity_variant", "Toxic Sheep"
        ));

        assertThat(formatted).isEqualTo("[Lv 7] Toxic Sheep");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat test --tests com.nick.mobrarity.visual.MobTextFormatterTest`

Expected: FAIL because visual classes do not exist.

- [ ] **Step 3: Add formatter**

```java
package com.nick.mobrarity.visual;

public enum DisplayMode {
    DISABLED,
    ALWAYS,
    WITHIN_DISTANCE,
    TARGETED
}
```

```java
package com.nick.mobrarity.visual;

import com.nick.mobrarity.integration.TextPlaceholderService;
import java.util.Map;

public final class MobTextFormatter {
    private final TextPlaceholderService placeholders;

    public MobTextFormatter(TextPlaceholderService placeholders) {
        this.placeholders = placeholders;
    }

    public String formatPlain(String template, Map<String, String> values) {
        return placeholders.replaceBuiltIns(template, values);
    }
}
```

- [ ] **Step 4: Add first command implementation**

```java
package com.nick.mobrarity.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public final class MobRarityCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("/mobrarity reload|inspect|set|spawn|clear");
            return true;
        }
        return true;
    }
}
```

- [ ] **Step 5: Wire command**

Update `RarityMobPlugin.onEnable()`:

```java
getCommand("mobrarity").setExecutor(new MobRarityCommand());
```

- [ ] **Step 6: Run tests**

Run: `.\gradlew.bat test --tests com.nick.mobrarity.visual.MobTextFormatterTest`

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/nick/mobrarity/visual src/main/java/com/nick/mobrarity/command src/main/java/com/nick/mobrarity/RarityMobPlugin.java src/test/java/com/nick/mobrarity/visual
git commit -m "feat: add visuals and admin command shell"
```

## Task 10: Build Verification And Manual Server Checklist

**Files:**
- Create: `docs/manual-test-checklist.md`
- Modify: `README.md`

- [ ] **Step 1: Add README**

Write `README.md`:

````markdown
# MobRarity

MobRarity is a Paper plugin for configurable mob rarities, named variants, event-driven effects, visual presentation, optional mob leveling, and soft integrations with LandClaims, VaultUnlocked, and PlaceholderAPI.

## Build

```powershell
.\gradlew.bat build
```

The plugin jar is written to `build/libs/MobRarity-1.0.0-SNAPSHOT.jar`.

## Runtime

- Paper 26.1+.
- Java 25.
- Optional: LandClaims, VaultUnlocked, PlaceholderAPI.
````

- [ ] **Step 2: Add manual checklist**

Write `docs/manual-test-checklist.md`:

```markdown
# Manual Test Checklist

- Start a Paper 26.1+ server with Java 25.
- Install `MobRarity-1.0.0-SNAPSHOT.jar` only.
- Confirm the plugin loads without LandClaims, VaultUnlocked, or PlaceholderAPI.
- Install PlaceholderAPI and confirm configured nametag text still renders.
- Install VaultUnlocked and confirm economy rewards do not warn at startup.
- After LandClaims exposes `LandClaimsApi`, confirm denied claim actions block Toxic Sheep aura and Rare Sheep shearing rewards.
- Use `/mobrarity spawn SHEEP rare_sheep <player>` and confirm a tagged sheep appears.
- Use `/mobrarity inspect` while targeting the sheep and confirm tier, variant, and level are reported.
- Shear Rare Sheep and confirm configured item rewards only occur from player shearing.
- Damage and kill a rarity mob through lava and confirm player reward effects do not run.
- Spawn mobs at increasing distance from world spawn and confirm levels increase.
- Spawn mobs below Y:64 and confirm depth bonus applies.
```

- [ ] **Step 3: Run full verification**

Run: `.\gradlew.bat clean test build`

Expected: PASS. The jar exists at `build/libs/MobRarity-1.0.0-SNAPSHOT.jar`.

- [ ] **Step 4: Commit**

```bash
git add README.md docs/manual-test-checklist.md
git commit -m "docs: add build and manual test checklist"
```

## Self-Review Notes

- Spec coverage: The plan covers Paper/Java 25 setup, commented YAML files, global tiers, per-mob variants, spawn-source controls, on-spawn/on-death style triggers, LandClaims fallback, VaultUnlocked adapter, PlaceholderAPI-aware text formatting, distance/depth leveling, admin commands, and manual verification.
- Config comments: Task 2 explicitly tests default YAML comment density and adds comments explaining function and gameplay impact.
- Risk: The LandClaims compile-only dependency depends on producing `libs/landclaims-api.jar` or publishing the API to a local Maven repository. The implementation should use the jar path first because the companion project is local and not yet published.
