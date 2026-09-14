package dev.maplesadventure.integration.subtleeffects;

import net.neoforged.fml.ModList;

/** Runtime gate for the optional SubtleEffects compatibility mixins. */
public final class SubtleEffectsIntegration {
    public static final String MOD_ID = "subtle_effects";

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    private SubtleEffectsIntegration() {
    }
}
