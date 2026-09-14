package dev.maplesadventure.soul;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.config.LostSoulConfig;
import dev.maplesadventure.registry.ModEntityTypes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/** Server-thread coordinator for death transactions and conservative spawn placement. */
public final class LostSoulManager {
    private static final Map<UUID, SafePosition> LAST_SAFE_POSITIONS = new HashMap<>();
    private static final Map<UUID, Integer> CAPTURED_DEATH_EXPERIENCE = new HashMap<>();
    private static final Set<UUID> PENDING_XP_RESETS = new HashSet<>();
    private static final double[][] SAFE_OFFSETS = {
            {0.0D, 0.0D, 0.0D},
            {0.0D, 0.5D, 0.0D}, {0.0D, 1.0D, 0.0D}, {0.0D, 1.5D, 0.0D}, {0.0D, 2.0D, 0.0D},
            {0.5D, 0.0D, 0.0D}, {-0.5D, 0.0D, 0.0D}, {0.0D, 0.0D, 0.5D}, {0.0D, 0.0D, -0.5D},
            {0.5D, 0.5D, 0.0D}, {-0.5D, 0.5D, 0.0D}, {0.0D, 0.5D, 0.5D}, {0.0D, 0.5D, -0.5D}
    };

    public static boolean createSoulForDeath(ServerPlayer player, int storedExperience) {
        ServerLevel level = player.serverLevel();
        MinecraftServer server = level.getServer();
        LostSoulSavedData data = LostSoulSavedData.get(server);
        UUID ownerUuid = player.getUUID();

        // Replacing the record first invalidates an unloaded old entity without loading its chunk.
        Optional<LostSoulRecord> previous = data.removeActive(ownerUuid);
        previous.ifPresent(record -> discardIfLoaded(server, record));

        LostSoulEntity soul = ModEntityTypes.LOST_SOUL.get().create(level);
        if (soul == null) {
            MaplesAdventure.LOGGER.error("Could not construct Lost Soul entity for {}", player.getGameProfile().getName());
            return false;
        }

        UUID soulId = UUID.randomUUID();
        long generation = data.nextGeneration();
        Vec3 intended = selectIntendedPosition(player);
        soul.moveTo(intended.x, intended.y, intended.z, 0.0F, 0.0F);
        Vec3 spawnPosition = findNearbyUnobstructedPosition(level, soul, intended);
        soul.moveTo(spawnPosition.x, spawnPosition.y, spawnPosition.z, 0.0F, 0.0F);

        LostSoulRecord record = new LostSoulRecord(
                ownerUuid,
                player.getGameProfile().getName(),
                soulId,
                level.dimension(),
                spawnPosition,
                Math.max(0, storedExperience),
                level.getGameTime(),
                generation
        );
        soul.initialize(record, player.getGameProfile(), LostSoulConfig.RECOVERY_DISTANCE.get().floatValue());

        if (!level.addFreshEntity(soul)) {
            MaplesAdventure.LOGGER.error(
                    "Could not add Lost Soul entity for {} at {} in {}. The previous soul remains invalidated.",
                    player.getGameProfile().getName(), spawnPosition, level.dimension().location()
            );
            return false;
        }
        data.setActive(record);
        PENDING_XP_RESETS.add(ownerUuid);
        MaplesAdventure.LOGGER.debug(
                "Created Lost Soul {} generation {} for {} with {} XP at {} in {}",
                soulId, generation, player.getGameProfile().getName(), storedExperience,
                spawnPosition, level.dimension().location()
        );
        return true;
    }

    public static void recordSafePosition(ServerPlayer player) {
        if (player.tickCount % 10 != 0 || !player.onGround() || player.isDeadOrDying()) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (player.getY() <= level.getMinBuildHeight() + 1 || !level.noCollision(player, player.getBoundingBox())) {
            return;
        }
        LAST_SAFE_POSITIONS.put(player.getUUID(), new SafePosition(level.dimension(), player.position()));
    }

    public static boolean hasPendingReset(UUID playerUuid) {
        return PENDING_XP_RESETS.contains(playerUuid);
    }

    public static void captureDeathExperience(ServerPlayer player) {
        CAPTURED_DEATH_EXPERIENCE.put(player.getUUID(), ExperiencePoints.capture(player));
    }

    public static int consumeCapturedDeathExperience(ServerPlayer player) {
        Integer captured = CAPTURED_DEATH_EXPERIENCE.remove(player.getUUID());
        return captured == null ? ExperiencePoints.capture(player) : captured;
    }

    public static void discardCapturedDeathExperience(UUID playerUuid) {
        CAPTURED_DEATH_EXPERIENCE.remove(playerUuid);
    }

    public static boolean consumePendingReset(UUID playerUuid) {
        return PENDING_XP_RESETS.remove(playerUuid);
    }

    public static void forgetPlayer(UUID playerUuid) {
        LAST_SAFE_POSITIONS.remove(playerUuid);
        CAPTURED_DEATH_EXPERIENCE.remove(playerUuid);
        PENDING_XP_RESETS.remove(playerUuid);
    }

    public static void clearTransientState() {
        LAST_SAFE_POSITIONS.clear();
        CAPTURED_DEATH_EXPERIENCE.clear();
        PENDING_XP_RESETS.clear();
    }

    private static Vec3 selectIntendedPosition(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 deathPosition = player.position();
        if (deathPosition.y >= level.getMinBuildHeight() + 1) {
            return deathPosition;
        }
        SafePosition safe = LAST_SAFE_POSITIONS.get(player.getUUID());
        if (safe != null && safe.dimension().equals(level.dimension())) {
            return safe.position();
        }
        int surfaceY = level.getHeight(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (int) Math.floor(deathPosition.x),
                (int) Math.floor(deathPosition.z)
        );
        return new Vec3(deathPosition.x, Math.max(level.getMinBuildHeight() + 1, surfaceY + 0.05D), deathPosition.z);
    }

    private static Vec3 findNearbyUnobstructedPosition(ServerLevel level, LostSoulEntity soul, Vec3 intended) {
        for (double[] offset : SAFE_OFFSETS) {
            Vec3 candidate = intended.add(offset[0], offset[1], offset[2]);
            soul.setPos(candidate);
            if (level.getWorldBorder().isWithinBounds(BlockPos.containing(candidate))
                    && level.noCollision(soul, soul.getBoundingBox())) {
                return candidate;
            }
        }
        return intended;
    }

    private static void discardIfLoaded(MinecraftServer server, LostSoulRecord record) {
        ServerLevel oldLevel = server.getLevel(record.dimension());
        if (oldLevel == null) {
            return;
        }
        Entity oldEntity = oldLevel.getEntity(record.soulId());
        if (oldEntity instanceof LostSoulEntity) {
            oldEntity.discard();
        }
    }

    private record SafePosition(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, Vec3 position) {
    }

    private LostSoulManager() {
    }
}
