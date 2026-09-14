package dev.maplesadventure.integration.epicfight;

import net.neoforged.fml.ModList;

/** Runtime gate shared by optional Epic Fight compatibility hooks. */
public final class EpicFightIntegration {
    public static final String MOD_ID = "epicfight";

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    private EpicFightIntegration() {
    }
}
