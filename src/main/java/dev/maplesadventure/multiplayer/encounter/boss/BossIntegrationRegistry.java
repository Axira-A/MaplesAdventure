package dev.maplesadventure.multiplayer.encounter.boss;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

/** Small optional-adapter registry. No third-party class is referenced by the core. */
public final class BossIntegrationRegistry {
    private static final List<BossIntegrationAdapter> ADAPTERS = new CopyOnWriteArrayList<>();

    public static void register(BossIntegrationAdapter adapter) {
        if (adapter != null && !ADAPTERS.contains(adapter)) ADAPTERS.add(adapter);
    }

    public static Optional<BossIntegrationAdapter> adapterFor(Entity entity) {
        return entity == null ? Optional.empty() : ADAPTERS.stream().filter(adapter -> adapter.supports(entity)).findFirst();
    }

    static Optional<Entity> classifyParent(Entity spawned) {
        return adapterFor(spawned).flatMap(adapter -> adapter.parentOf(spawned));
    }

    static Optional<BossSpawnClassification> classify(Entity spawned) {
        return adapterFor(spawned).flatMap(adapter -> adapter.classifySpawnedEntity(spawned));
    }

    public static void onAttemptStarted(Mob primary) {
        adapterFor(primary).ifPresent(adapter -> adapter.onAttemptStarted(primary));
    }
    public static void onStageChanged(Mob primary, int stage) {
        adapterFor(primary).ifPresent(adapter -> adapter.onStageChanged(primary, stage));
    }
    public static void onReplacement(Mob oldPrimary, Mob newPrimary, int stage) {
        adapterFor(oldPrimary).or(() -> adapterFor(newPrimary))
                .ifPresent(adapter -> adapter.onPrimaryReplacement(oldPrimary, newPrimary, stage));
    }
    public static void onAttemptReset(Mob primary) {
        adapterFor(primary).ifPresent(adapter -> adapter.onAttemptReset(primary));
    }
    public static void onDefeated(Mob primary) {
        adapterFor(primary).ifPresent(adapter -> adapter.onDefeated(primary));
    }
    public static NativeBossBarPolicy nativeBossBarPolicy(Mob primary) {
        return adapterFor(primary).map(adapter -> adapter.nativeBossBarPolicy(primary))
                .orElse(NativeBossBarPolicy.KEEP);
    }

    private BossIntegrationRegistry() {}
}
