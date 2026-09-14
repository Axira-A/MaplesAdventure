package dev.maplesadventure.multiplayer.hub;

import dev.maplesadventure.multiplayer.phase.PhaseRole;

/** Small server-authored menu projection; it is presentation state, never a security boundary. */
public record MultiplayerHubState(
        PhaseRole role,
        boolean coopSession,
        boolean hostileSession,
        boolean invasionQueued,
        boolean coopSign,
        boolean duelSign,
        boolean bossActive,
        boolean messageAvailable,
        boolean signAvailable,
        boolean invasionAvailable,
        String cooperatorName,
        String invaderName
) {
    public static MultiplayerHubState empty() {
        return new MultiplayerHubState(PhaseRole.SOLO, false, false, false, false, false, false,
                false, false, false, "", "");
    }
}
