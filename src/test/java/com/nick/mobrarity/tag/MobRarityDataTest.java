package com.nick.mobrarity.tag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

final class MobRarityDataTest {
    @Test
    void serializesAndParsesData() {
        MobRarityData data = new MobRarityData("rare", "rare_sheep", 14);

        assertThat(MobRarityData.parse(data.serialize())).contains(data);
    }

    @Test
    void rejectsMalformedData() {
        assertThat(MobRarityData.parse("rare|rare_sheep")).isEmpty();
        assertThat(MobRarityData.parse("rare|rare_sheep|nope")).isEmpty();
    }

    @Test
    void rejectsInvalidLevel() {
        assertThat(MobRarityData.parse("rare|rare_sheep|0")).isEmpty();
        assertThat(MobRarityData.parse("rare|rare_sheep|-1")).isEmpty();
    }

    @Test
    void rejectsInvalidConstructedValues() {
        assertThatNullPointerException().isThrownBy(() -> new MobRarityData(null, "rare_sheep", 1));
        assertThatNullPointerException().isThrownBy(() -> new MobRarityData("rare", null, 1));
        assertThatThrownBy(() -> new MobRarityData("", "rare_sheep", 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MobRarityData(" ", "rare_sheep", 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MobRarityData("rare", "", 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MobRarityData("rare", " ", 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MobRarityData("ra|re", "rare_sheep", 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MobRarityData("rare", "rare|sheep", 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MobRarityData("rare", "rare_sheep", 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullAndExtraFieldParse() {
        assertThat(MobRarityData.parse(null)).isEmpty();
        assertThat(MobRarityData.parse("rare|variant|1|extra")).isEmpty();
        assertThat(MobRarityData.parse("ra|re|variant|1")).isEmpty();
    }
}
