package dev.maplesadventure.multiplayer.phase.mob;

import dev.maplesadventure.multiplayer.phase.PhaseId;
import dev.maplesadventure.multiplayer.phase.PhaseMembership;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Creates and manages only command-spawned prototype zombies. */
public final class PhaseMobManager {
    private static final List<Double> SPAWN_DISTANCES = List.of(2.0D, 1.25D, 0.0D);

    public static Optional<Zombie> spawnZombie(ServerPlayer executor, PhaseId phaseId) {
        ServerLevel level = executor.serverLevel();
        Vec3 view = executor.getViewVector(1.0F);
        Vec3 horizontal = new Vec3(view.x, 0.0D, view.z);
        if (horizontal.lengthSqr() < 1.0E-6D) horizontal = new Vec3(0.0D, 0.0D, 1.0D);
        horizontal = horizontal.normalize();

        for (double distance : SPAWN_DISTANCES) {
            Vec3 candidate = new Vec3(
                    executor.getX() + horizontal.x * distance,
                    executor.getBoundingBox().minY,
                    executor.getZ() + horizontal.z * distance
            );
            Zombie zombie = EntityType.ZOMBIE.create(level);
            if (zombie == null) return Optional.empty();
            zombie.moveTo(candidate.x, candidate.y, candidate.z, executor.getYRot() + 180.0F, 0.0F);
            BlockPos feet = zombie.blockPosition();
            AABB box = zombie.getBoundingBox();
            if (!level.isLoaded(feet) || !level.getWorldBorder().isWithinBounds(feet)
                    || !level.getFluidState(feet).isEmpty() || !level.noBlockCollision(zombie, box)) continue;

            zombie.setPersistenceRequired();
            zombie.setData(ModPhaseAttachments.MOB_PHASE, MobPhaseState.prototype(phaseId));
            if (level.addFreshEntity(zombie)) return Optional.of(zombie);
            return Optional.empty();
        }
        return Optional.empty();
    }

    public static Optional<Zombie> findForInspection(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F).normalize();
        return player.serverLevel().getEntitiesOfClass(
                        Zombie.class, player.getBoundingBox().inflate(16.0D), PhaseMembership::isPrototypePhasedMob)
                .stream()
                .min(Comparator.comparingDouble(zombie -> inspectionScore(eye, look, zombie)));
    }

    public static int clearPrototypes(MinecraftServer server) {
        int removed = 0;
        for (ServerLevel level : server.getAllLevels()) {
            for (Zombie zombie : level.getEntities(EntityType.ZOMBIE,
                    PhaseMembership::isPrototypePhasedMob)) {
                zombie.discard();
                removed++;
            }
        }
        return removed;
    }

    private static double inspectionScore(Vec3 eye, Vec3 look, Zombie zombie) {
        Vec3 delta = zombie.getBoundingBox().getCenter().subtract(eye);
        double distance = Math.max(0.001D, delta.length());
        double alignmentPenalty = 1.0D - Math.max(-1.0D, Math.min(1.0D, look.dot(delta.scale(1.0D / distance))));
        return distance + alignmentPenalty * 8.0D;
    }

    private PhaseMobManager() {}
}
