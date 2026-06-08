package com.nick.mobrarity.level;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void calculatesUsingFractionalYCoordinate() {
        LevelSettings settings = new LevelSettings(true, 250, 1, 100, true, 64, 2.0, 25);
        MobLevelService service = new MobLevelService(settings);

        assertThat(service.calculateLevel(0, 63.5)).isEqualTo(2);
    }

    @Test
    void rejectsNonPositiveBlocksPerLevelWhenEnabled() {
        assertThatThrownBy(() -> new LevelSettings(true, 0, 1, 100, true, 64, 0.15, 25))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("blocksPerLevel must be positive when leveling is enabled");
    }

    @Test
    void rejectsMinGreaterThanMax() {
        assertThatThrownBy(() -> new LevelSettings(true, 250, 101, 100, true, 64, 0.15, 25))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("minLevel must be less than or equal to maxLevel");
    }

    @Test
    void rejectsNegativeDepthBonusLimit() {
        assertThatThrownBy(() -> new LevelSettings(true, 250, 1, 100, true, 64, 0.15, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxBonusLevels must be non-negative");
    }

    @Test
    void rejectsNegativeOrNonFiniteLevelsPerYWhenDepthEnabled() {
        assertThatThrownBy(() -> new LevelSettings(true, 250, 1, 100, true, 64, -0.1, 25))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("levelsPerY must be finite and non-negative when depth bonus is enabled");

        assertThatThrownBy(() -> new LevelSettings(true, 250, 1, 100, true, 64, Double.NaN, 25))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("levelsPerY must be finite and non-negative when depth bonus is enabled");

        assertThatThrownBy(() -> new LevelSettings(true, 250, 1, 100, true, 64, Double.POSITIVE_INFINITY, 25))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("levelsPerY must be finite and non-negative when depth bonus is enabled");
    }
}
