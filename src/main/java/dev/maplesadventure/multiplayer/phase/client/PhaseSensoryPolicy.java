package dev.maplesadventure.multiplayer.phase.client;

import dev.maplesadventure.multiplayer.phase.PhaseRelations;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Client-side policy boundary for sensory information produced by phase-bearing entities.
 * Shared entities and visual-only Echo playbacks remain exposed.
 */
public final class PhaseSensoryPolicy {
    public static boolean shouldExposePlayerToLocalClient(Player source) {
        Player localPlayer = Minecraft.getInstance().player;
        return localPlayer == null || source == localPlayer || PhaseRelations.canSee(localPlayer, source);
    }

    public static boolean shouldExposeEntitySource(Entity source) {
        Player localPlayer = Minecraft.getInstance().player;
        return localPlayer == null || source == localPlayer || PhaseRelations.canSee(localPlayer, source);
    }

    private PhaseSensoryPolicy() {
    }
}
