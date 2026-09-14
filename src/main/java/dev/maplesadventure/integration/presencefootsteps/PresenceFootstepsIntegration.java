package dev.maplesadventure.integration.presencefootsteps;

import net.neoforged.fml.ModList;

/** Runtime gate for the optional Presence Footsteps compatibility mixin. */
public final class PresenceFootstepsIntegration {
    public static final String MOD_ID = "presencefootsteps";

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    private PresenceFootstepsIntegration() {
    }
}
