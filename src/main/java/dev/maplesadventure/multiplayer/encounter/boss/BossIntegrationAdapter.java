package dev.maplesadventure.multiplayer.encounter.boss;

import java.util.Optional;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

/**
 * Optional semantic bridge for one boss family. Adapters describe ownership/stages; the core alone mutates
 * phase, encounter and SavedData state.
 */
public interface BossIntegrationAdapter {
    boolean supports(Entity entity);

    default void onAttemptStarted(Mob primary) {}
    default void onStageChanged(Mob primary, int stage) {}
    default void onPrimaryReplacement(Mob oldPrimary, Mob newPrimary, int stage) {}
    default Optional<Entity> parentOf(Entity spawned) { return Optional.empty(); }
    default Optional<BossSpawnClassification> classifySpawnedEntity(Entity spawned) { return Optional.empty(); }
    default NativeBossBarPolicy nativeBossBarPolicy(Mob primary) { return NativeBossBarPolicy.KEEP; }
    default void onAttemptReset(Mob primary) {}
    default void onDefeated(Mob primary) {}
}
