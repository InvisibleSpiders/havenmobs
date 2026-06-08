package com.nick.mobrarity.level;

public record LevelSettings(
        boolean enabled,
        int blocksPerLevel,
        int minLevel,
        int maxLevel,
        boolean depthBonusEnabled,
        int startsBelowY,
        double levelsPerY,
        int maxBonusLevels) {
    public LevelSettings {
        if (enabled && blocksPerLevel <= 0) {
            throw new IllegalArgumentException("blocksPerLevel must be positive when leveling is enabled");
        }
        if (minLevel > maxLevel) {
            throw new IllegalArgumentException("minLevel must be less than or equal to maxLevel");
        }
        if (maxBonusLevels < 0) {
            throw new IllegalArgumentException("maxBonusLevels must be non-negative");
        }
        if (depthBonusEnabled && (!Double.isFinite(levelsPerY) || levelsPerY < 0)) {
            throw new IllegalArgumentException("levelsPerY must be finite and non-negative when depth bonus is enabled");
        }
    }
}
