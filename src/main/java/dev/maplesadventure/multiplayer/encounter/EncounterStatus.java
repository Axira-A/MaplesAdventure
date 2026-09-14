package dev.maplesadventure.multiplayer.encounter;

public enum EncounterStatus {
    READY,
    ACTIVE,
    CLEARED,
    DEFEATED;

    public boolean validFor(EncounterType type) {
        return type == EncounterType.BOSS ? this != CLEARED : this != DEFEATED;
    }
}
