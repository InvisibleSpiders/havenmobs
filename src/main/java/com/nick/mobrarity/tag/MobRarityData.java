package com.nick.mobrarity.tag;

import java.util.Objects;
import java.util.Optional;

public record MobRarityData(String tierKey, String variantKey, int level) {
    private static final String DELIMITER = "|";
    private static final String DELIMITER_PATTERN = "\\|";

    public MobRarityData {
        Objects.requireNonNull(tierKey, "tierKey");
        Objects.requireNonNull(variantKey, "variantKey");
        validateKey(tierKey, "tierKey");
        validateKey(variantKey, "variantKey");
        if (level < 1) {
            throw new IllegalArgumentException("level must be at least 1");
        }
    }

    public String serialize() {
        return String.join(DELIMITER, tierKey, variantKey, Integer.toString(level));
    }

    public static Optional<MobRarityData> parse(String value) {
        if (value == null) {
            return Optional.empty();
        }

        String[] parts = value.split(DELIMITER_PATTERN, -1);
        if (parts.length != 3 || parts[0].isBlank() || parts[1].isBlank()) {
            return Optional.empty();
        }

        try {
            int level = Integer.parseInt(parts[2]);
            if (level < 1) {
                return Optional.empty();
            }
            return Optional.of(new MobRarityData(parts[0], parts[1], level));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private static void validateKey(String value, String fieldName) {
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        if (value.contains(DELIMITER)) {
            throw new IllegalArgumentException(fieldName + " must not contain '" + DELIMITER + "'");
        }
    }
}
