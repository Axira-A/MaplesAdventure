package dev.maplesadventure.multiplayer.encounter.boss;

/** Adapter declaration for one reliably parented entity spawned by a managed boss. */
public record BossSpawnClassification(BossEntityRole role, boolean completionRelevant) {
    public BossSpawnClassification {
        if (role == null || role == BossEntityRole.PRIMARY || role == BossEntityRole.ADD) {
            throw new IllegalArgumentException("Spawned boss derivatives must be CHILD/PROJECTILE/EFFECT/PART");
        }
    }

    public static BossSpawnClassification child(boolean completionRelevant) {
        return new BossSpawnClassification(BossEntityRole.CHILD, completionRelevant);
    }
    public static BossSpawnClassification projectile() {
        return new BossSpawnClassification(BossEntityRole.PROJECTILE, false);
    }
    public static BossSpawnClassification effect() {
        return new BossSpawnClassification(BossEntityRole.EFFECT, false);
    }
    public static BossSpawnClassification part() {
        return new BossSpawnClassification(BossEntityRole.PART, false);
    }
}
