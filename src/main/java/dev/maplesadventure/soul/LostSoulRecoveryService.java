package dev.maplesadventure.soul;

import dev.maplesadventure.config.LostSoulConfig;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Validates and atomically claims a soul entirely on the logical server. */
public final class LostSoulRecoveryService {
    private static final double EPSILON = 1.0E-6D;

    public static boolean recover(ServerPlayer player, LostSoulEntity soul) {
        if (!LostSoulConfig.ENABLED.get() || soul.isRemoved() || !player.isAlive()) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        if (soul.level() != level || !level.getWorldBorder().isWithinBounds(soul.blockPosition())) {
            return false;
        }
        UUID ownerUuid = soul.getOwnerUuid();
        UUID soulId = soul.getSoulId();
        if (ownerUuid == null || soulId == null || !ownerUuid.equals(player.getUUID())) {
            return false;
        }

        LostSoulSavedData data = LostSoulSavedData.get(level.getServer());
        Optional<LostSoulRecord> active = data.getActive(ownerUuid);
        if (active.isEmpty() || !matches(active.get(), soul, level)) {
            soul.discard();
            return false;
        }

        double preciseDistance = distanceBetween(player.getBoundingBox(), soul.getBoundingBox());
        double configuredMaximum = Math.min(LostSoulConfig.RECOVERY_DISTANCE.get(), soul.getRecoveryDistance());
        if (preciseDistance > configuredMaximum + EPSILON || !player.canInteractWithEntity(soul, 0.0D)) {
            return false;
        }
        if (!hasLineOfSight(level, player, soul)) {
            return false;
        }

        // The compare-and-remove happens before discard and XP restoration. A repeated packet cannot claim twice.
        Optional<LostSoulRecord> claimed = data.claim(ownerUuid, soulId, soul.getGeneration());
        if (claimed.isEmpty()) {
            return false;
        }
        LostSoulRecord record = claimed.get();
        Vec3 effectPosition = soul.position().add(0.0D, 0.75D, 0.0D);
        soul.discard();
        ExperiencePoints.setExact(player, record.storedExperience());
        level.sendParticles(ParticleTypes.SOUL, effectPosition.x, effectPosition.y, effectPosition.z, 18, 0.35D, 0.45D, 0.35D, 0.025D);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, effectPosition.x, effectPosition.y, effectPosition.z, 10, 0.25D, 0.25D, 0.25D, 0.015D);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.85F, 0.82F);
        player.displayClientMessage(Component.translatable("message.maplesadventure.soul_recovered"), true);
        return true;
    }

    private static boolean matches(LostSoulRecord record, LostSoulEntity soul, ServerLevel level) {
        return record.soulId().equals(soul.getSoulId())
                && record.generation() == soul.getGeneration()
                && record.dimension().equals(level.dimension())
                && soul.getUUID().equals(record.soulId());
    }

    private static boolean hasLineOfSight(ServerLevel level, ServerPlayer player, LostSoulEntity soul) {
        AABB bounds = soul.getBoundingBox();
        Vec3 eye = player.getEyePosition();
        double x = clamp(eye.x, bounds.minX, bounds.maxX);
        double z = clamp(eye.z, bounds.minZ, bounds.maxZ);
        double[] heights = {0.72D, 0.50D, 0.90D, 0.25D};
        for (double height : heights) {
            Vec3 target = new Vec3(x, bounds.minY + (bounds.maxY - bounds.minY) * height, z);
            if (eye.distanceToSqr(target) <= EPSILON * EPSILON) {
                return true;
            }
            BlockHitResult hit = level.clip(new ClipContext(
                    eye, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player
            ));
            if (hit.getType() == HitResult.Type.MISS) {
                return true;
            }
        }
        return false;
    }

    private static double distanceBetween(AABB first, AABB second) {
        double x = Math.max(Math.max(first.minX - second.maxX, second.minX - first.maxX), 0.0D);
        double y = Math.max(Math.max(first.minY - second.maxY, second.minY - first.maxY), 0.0D);
        double z = Math.max(Math.max(first.minZ - second.maxZ, second.minZ - first.maxZ), 0.0D);
        return Math.sqrt(x * x + y * y + z * z);
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private LostSoulRecoveryService() {
    }
}
