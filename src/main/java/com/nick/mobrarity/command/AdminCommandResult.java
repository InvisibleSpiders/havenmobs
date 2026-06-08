package com.nick.mobrarity.command;

import java.util.Objects;

public record AdminCommandResult(boolean success, String message) {
    public AdminCommandResult {
        Objects.requireNonNull(message, "message");
    }

    public static AdminCommandResult success(String message) {
        return new AdminCommandResult(true, message);
    }

    public static AdminCommandResult failure(String message) {
        return new AdminCommandResult(false, message);
    }
}
