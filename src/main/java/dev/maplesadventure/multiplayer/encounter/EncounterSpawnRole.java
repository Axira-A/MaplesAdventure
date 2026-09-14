package dev.maplesadventure.multiplayer.encounter;

/** Semantic role of one configured encounter spawn point. */
public enum EncounterSpawnRole {
    NORMAL,
    BOSS_PRIMARY,
    BOSS_ADD,
    BOSS_CHILD;

    public boolean isPrimary() { return this == BOSS_PRIMARY; }
}
