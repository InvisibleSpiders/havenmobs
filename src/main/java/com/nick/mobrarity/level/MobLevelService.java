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

        int level = settings.minLevel() + (int) Math.floor(horizontalDistance / settings.blocksPerLevel());
        if (settings.depthBonusEnabled() && y < settings.startsBelowY()) {
            int bonus = (int) Math.floor((settings.startsBelowY() - y) * settings.levelsPerY());
            level += Math.min(settings.maxBonusLevels(), bonus);
        }

        return Math.clamp(level, settings.minLevel(), settings.maxLevel());
    }
}
