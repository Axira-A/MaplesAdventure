package dev.maplesadventure.multiplayer.phase;

/** Server-authored state. Phase one intentionally contains no session persistence fields. */
public record PlayerPhaseState(PhaseId phaseId, PhaseRole role) {
    public PlayerPhaseState {
        if (phaseId == null || role == null) throw new IllegalArgumentException("Phase state fields cannot be null");
    }

    public static PlayerPhaseState solo(java.util.UUID playerUuid) {
        return new PlayerPhaseState(PhaseId.solo(playerUuid), PhaseRole.SOLO);
    }
}
