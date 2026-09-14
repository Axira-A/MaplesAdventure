package dev.maplesadventure.multiplayer.echo.client;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.config.EchoClientConfig;
import dev.maplesadventure.multiplayer.echo.network.EchoPayloads;
import dev.maplesadventure.multiplayer.phase.client.ClientPhaseState;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;

public final class EchoPlaybackManager {
    private static final int MAX_ACTIVE = 2;
    private static final Map<UUID, EchoPlayback> ACTIVE = new LinkedHashMap<>();

    public static void accept(EchoPayloads.Playback payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!EchoClientConfig.ENABLED.get() || minecraft.level == null
                || !minecraft.level.dimension().location().equals(payload.dimension())) return;
        while (ACTIVE.size() >= MAX_ACTIVE) ACTIVE.remove(ACTIVE.keySet().iterator().next());
        ACTIVE.put(payload.playbackId(), new EchoPlayback(payload.playbackId(), payload.dimension(),
                payload.appearance(), payload.frames(), payload.fadeInTicks(), payload.fadeOutTicks()));
        if (EchoClientConfig.DEBUG.get()) MaplesAdventure.LOGGER.info(
                "[MaplesAdventure/Echo] playback started id={} source={} frames={}",
                payload.playbackId(), payload.appearance().playerId(), payload.frames().size());
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || !EchoClientConfig.ENABLED.get()) {
            clear();
            return;
        }
        ACTIVE.values().removeIf(playback -> {
            if (!minecraft.level.dimension().location().equals(playback.dimension())) return true;
            if (ClientPhaseState.state(minecraft.player.getUUID()).phaseId().equals(
                    ClientPhaseState.state(playback.appearance().playerId()).phaseId())) {
                debugEnd(playback, "same_phase");
                return true;
            }
            playback.tick();
            if (playback.finished()) {
                debugEnd(playback, "finished");
                return true;
            }
            return false;
        });
    }

    static Collection<EchoPlayback> active() { return ACTIVE.values(); }
    public static void removeSource(UUID sourceUuid) {
        ACTIVE.values().removeIf(playback -> playback.appearance().playerId().equals(sourceUuid));
    }

    public static void removeSourceIfVisible(UUID sourceUuid) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && ClientPhaseState.state(minecraft.player.getUUID()).phaseId().equals(
                ClientPhaseState.state(sourceUuid).phaseId())) removeSource(sourceUuid);
    }
    public static void clear() { ACTIVE.clear(); }

    private static void debugEnd(EchoPlayback playback, String reason) {
        if (EchoClientConfig.DEBUG.get()) MaplesAdventure.LOGGER.info(
                "[MaplesAdventure/Echo] playback ended id={} reason={}", playback.playbackId(), reason);
    }

    private EchoPlaybackManager() {}
}
