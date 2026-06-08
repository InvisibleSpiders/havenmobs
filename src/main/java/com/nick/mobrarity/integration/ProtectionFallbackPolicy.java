package com.nick.mobrarity.integration;

public enum ProtectionFallbackPolicy {
    ALLOW,
    DENY_PROTECTED_EFFECTS,
    DENY_ALL_EFFECTS;

    public boolean allowsMissingService(String actionType) {
        return switch (this) {
            case ALLOW -> true;
            case DENY_ALL_EFFECTS -> false;
            case DENY_PROTECTED_EFFECTS -> isVisualAction(actionType);
        };
    }

    private static boolean isVisualAction(String actionType) {
        return "message".equals(actionType) || "particle".equals(actionType) || "sound".equals(actionType);
    }
}
