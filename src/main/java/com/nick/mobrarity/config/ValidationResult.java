package com.nick.mobrarity.config;

import java.util.List;

public record ValidationResult(boolean valid, List<String> messages) {
    public ValidationResult {
        messages = List.copyOf(messages);
    }

    public static ValidationResult success() {
        return new ValidationResult(true, List.of());
    }

    public static ValidationResult invalid(String message) {
        return new ValidationResult(false, List.of(message));
    }
}
