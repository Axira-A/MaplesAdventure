package dev.maplesadventure.integration.epicfight;

import dev.maplesadventure.multiplayer.phase.client.PhaseSensoryPolicy;
import yesman.epicfight.world.capabilities.entitypatch.EntityPatch;

/**
 * Typed Epic Fight boundary used only after the optional mod gate has succeeded.
 * Keeping Epic Fight types here prevents common phase code from linking its API.
 */
public final class EpicFightSensoryBridge {
    public static boolean shouldExposePatch(Object candidate) {
        if (!(candidate instanceof EntityPatch<?> patch)) {
            return true;
        }
        return PhaseSensoryPolicy.shouldExposeEntitySource(patch.getOriginal());
    }

    private EpicFightSensoryBridge() {
    }
}
