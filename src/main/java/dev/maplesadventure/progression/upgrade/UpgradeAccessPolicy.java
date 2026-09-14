package dev.maplesadventure.progression.upgrade;

import dev.maplesadventure.multiplayer.phase.PhaseRole;

/** Shared gameplay permission independent of whether access came from a bonfire or future NPC. */
public final class UpgradeAccessPolicy {
    public static boolean allows(PhaseRole role, boolean alive, boolean spectator,
                                 boolean bossActive, boolean hostileSession) {
        return alive && !spectator && !bossActive && !hostileSession
                && (role == PhaseRole.SOLO || role == PhaseRole.HOST);
    }

    private UpgradeAccessPolicy() {}
}
