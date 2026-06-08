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
        double totalWeight = entries.stream()
                .mapToDouble(Entry::weight)
                .filter(weight -> weight > 0)
                .sum();
        if (totalWeight <= 0) {
            return Optional.empty();
        }

        double target = random.nextDouble(totalWeight);
        double cumulativeWeight = 0;
        for (Entry<T> entry : entries) {
            if (entry.weight() <= 0) {
                continue;
            }

            cumulativeWeight += entry.weight();
            if (target < cumulativeWeight) {
                return Optional.of(entry.value());
            }
        }

        return Optional.empty();
    }

    public record Entry<T>(T value, double weight) {}
}
