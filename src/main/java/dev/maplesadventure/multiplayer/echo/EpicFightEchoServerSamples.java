package dev.maplesadventure.multiplayer.echo;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Latest visual-only animation report from each player's own client. Never used for gameplay authority. */
public final class EpicFightEchoServerSamples {
    private static final int MAX_AGE_TICKS = 8;
    private static final Map<UUID, Sample> SAMPLES = new HashMap<>();

    public static void accept(UUID playerId, int serverTick, EpicFightEchoFrameState state) {
        SAMPLES.put(playerId, new Sample(serverTick, state));
    }

    public static EpicFightEchoFrameState latest(UUID playerId, int serverTick) {
        Sample sample = SAMPLES.get(playerId);
        return sample != null && serverTick - sample.serverTick <= MAX_AGE_TICKS
                ? sample.state : EpicFightEchoFrameState.INACTIVE;
    }

    public static void remove(UUID playerId) { SAMPLES.remove(playerId); }
    public static void clear() { SAMPLES.clear(); }
    private record Sample(int serverTick, EpicFightEchoFrameState state) {}
    private EpicFightEchoServerSamples() {}
}
