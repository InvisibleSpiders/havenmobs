package com.nick.mobrarity.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class ProtectionFallbackPolicyTest {
    @Test
    void allowAllowsEveryEffectWhenServiceIsMissing() {
        assertThat(ProtectionFallbackPolicy.ALLOW.allowsMissingService("message")).isTrue();
        assertThat(ProtectionFallbackPolicy.ALLOW.allowsMissingService("particle")).isTrue();
        assertThat(ProtectionFallbackPolicy.ALLOW.allowsMissingService("sound")).isTrue();
        assertThat(ProtectionFallbackPolicy.ALLOW.allowsMissingService("item_drop")).isTrue();
    }

    @Test
    void denyAllEffectsDeniesEveryEffectWhenServiceIsMissing() {
        assertThat(ProtectionFallbackPolicy.DENY_ALL_EFFECTS.allowsMissingService("message")).isFalse();
        assertThat(ProtectionFallbackPolicy.DENY_ALL_EFFECTS.allowsMissingService("particle")).isFalse();
        assertThat(ProtectionFallbackPolicy.DENY_ALL_EFFECTS.allowsMissingService("sound")).isFalse();
        assertThat(ProtectionFallbackPolicy.DENY_ALL_EFFECTS.allowsMissingService("item_drop")).isFalse();
    }

    @Test
    void denyProtectedEffectsStillAllowsVisualsAndAudio() {
        assertThat(ProtectionFallbackPolicy.DENY_PROTECTED_EFFECTS.allowsMissingService("message")).isTrue();
        assertThat(ProtectionFallbackPolicy.DENY_PROTECTED_EFFECTS.allowsMissingService("particle")).isTrue();
        assertThat(ProtectionFallbackPolicy.DENY_PROTECTED_EFFECTS.allowsMissingService("sound")).isTrue();
        assertThat(ProtectionFallbackPolicy.DENY_PROTECTED_EFFECTS.allowsMissingService("item_drop")).isFalse();
    }
}
