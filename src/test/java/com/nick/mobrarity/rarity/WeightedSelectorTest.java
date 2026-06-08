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
