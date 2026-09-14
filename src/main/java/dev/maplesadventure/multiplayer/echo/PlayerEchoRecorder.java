package dev.maplesadventure.multiplayer.echo;

import dev.maplesadventure.config.EchoServerConfig;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class PlayerEchoRecorder {
    /** Covers a 12-second playback plus delay even at the allowed one-tick sample interval. */
    public static final int MAX_BUFFER_FRAMES = 320;
    public static final int MAX_PLAYBACK_FRAMES = 256;
    private static final double DISCONTINUITY_SQR = 64.0D;
    private static final Map<UUID, EchoTrajectory> TRAJECTORIES = new HashMap<>();
    private static final Map<UUID, MotionState> MOTION = new HashMap<>();

    public static void sample(MinecraftServer server) {
        int interval = EchoServerConfig.SAMPLE_INTERVAL_TICKS.get();
        int now = server.getTickCount();
        if (now % interval != 0) return;
        int durationTicks = (int) Math.ceil((EchoServerConfig.HISTORY_SECONDS.get()
                + EchoServerConfig.PLAYBACK_DELAY_SECONDS.get()) * 20.0D);
        int capacity = Math.min(MAX_BUFFER_FRAMES, Math.max(4, durationTicks / interval + 2));
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            MotionState motion = updateMotion(player, interval);
            EchoFrame frame = new EchoFrame(now, player.getX(), player.getY(), player.getZ(),
                    player.getYRot(), player.getXRot(), player.yBodyRot, player.yHeadRot,
                    motion.walkPosition, motion.walkSpeed, player.getAttackAnim(0.0F),
                    (byte) player.getPose().ordinal(), player.onGround(),
                    EpicFightEchoServerSamples.latest(player.getUUID(), now));
            TRAJECTORIES.computeIfAbsent(player.getUUID(), ignored -> new EchoTrajectory())
                    .add(player.level().dimension(), frame, capacity, DISCONTINUITY_SQR);
        }
    }

    private static MotionState updateMotion(ServerPlayer player, int interval) {
        Vec3 position = player.position();
        MotionState previous = MOTION.get(player.getUUID());
        if (previous == null || previous.dimension != player.level().dimension()
                || previous.position.distanceToSqr(position) > DISCONTINUITY_SQR) {
            MotionState fresh = new MotionState(player.level().dimension(), position, 0.0F, 0.0F);
            MOTION.put(player.getUUID(), fresh);
            return fresh;
        }
        double dx = position.x - previous.position.x;
        double dz = position.z - previous.position.z;
        float perTickDistance = (float) (Math.sqrt(dx * dx + dz * dz) / Math.max(1, interval));
        float targetSpeed = Math.min(perTickDistance * 4.0F, 1.0F);
        float damping = 1.0F - (float) Math.pow(0.6D, interval);
        float speed = previous.walkSpeed + (targetSpeed - previous.walkSpeed) * damping;
        float walkPosition = previous.walkPosition + speed * interval;
        MotionState updated = new MotionState(player.level().dimension(), position, walkPosition, speed);
        MOTION.put(player.getUUID(), updated);
        return updated;
    }

    public static List<EchoFrame> playback(ServerPlayer player, int nowTick) {
        EchoTrajectory trajectory = TRAJECTORIES.get(player.getUUID());
        if (trajectory == null || trajectory.dimension() != player.level().dimension()) return List.of();
        int delay = (int) Math.round(EchoServerConfig.PLAYBACK_DELAY_SECONDS.get() * 20.0D);
        int history = (int) Math.round(EchoServerConfig.HISTORY_SECONDS.get() * 20.0D);
        return trajectory.delayedFrames(nowTick, delay, history, MAX_PLAYBACK_FRAMES);
    }

    public static int frameCount(UUID playerId) {
        EchoTrajectory trajectory = TRAJECTORIES.get(playerId);
        return trajectory == null ? 0 : trajectory.size();
    }
    public static void cut(UUID playerId) {
        TRAJECTORIES.remove(playerId); MOTION.remove(playerId); EpicFightEchoServerSamples.remove(playerId);
    }
    public static void clear() { TRAJECTORIES.clear(); MOTION.clear(); EpicFightEchoServerSamples.clear(); }
    private record MotionState(ResourceKey<Level> dimension, Vec3 position, float walkPosition, float walkSpeed) {}
    private PlayerEchoRecorder() {}
}
