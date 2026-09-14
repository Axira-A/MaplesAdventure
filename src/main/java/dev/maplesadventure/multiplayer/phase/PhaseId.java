package dev.maplesadventure.multiplayer.phase;

import java.util.UUID;

/** Stable, language-neutral identity for a multiplayer phase. */
public record PhaseId(UUID value) {
    public PhaseId {
        if (value == null) throw new IllegalArgumentException("Phase UUID cannot be null");
    }

    public static PhaseId solo(UUID playerUuid) {
        return new PhaseId(playerUuid);
    }

    @Override public String toString() { return value.toString(); }
}
