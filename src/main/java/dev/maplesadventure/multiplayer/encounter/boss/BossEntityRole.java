package dev.maplesadventure.multiplayer.encounter.boss;

/** Semantic role inside one boss-attempt entity lineage. */
public enum BossEntityRole {
    PRIMARY,
    ADD,
    CHILD,
    PROJECTILE,
    EFFECT,
    PART;

    public boolean persistentMember() {
        return this == PRIMARY || this == ADD || this == CHILD;
    }

    public boolean transientMember() {
        return this == PROJECTILE || this == EFFECT || this == PART;
    }
}
