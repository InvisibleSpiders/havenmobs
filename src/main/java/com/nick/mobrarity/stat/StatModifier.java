package com.nick.mobrarity.stat;

public record StatModifier(double add, double multiply, double perLevel) {
    public StatModifier {
        if (!Double.isFinite(add)) {
            add = 0.0;
        }
        if (!Double.isFinite(multiply) || multiply <= 0) {
            multiply = 1.0;
        }
        if (!Double.isFinite(perLevel)) {
            perLevel = 0.0;
        }
    }

    public double apply(double baseValue, int level) {
        return (baseValue * multiply) + add + (perLevel * level);
    }
}
