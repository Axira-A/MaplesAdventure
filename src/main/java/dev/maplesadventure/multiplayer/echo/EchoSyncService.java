package dev.maplesadventure.multiplayer.echo;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.config.EchoServerConfig;
import dev.maplesadventure.multiplayer.echo.network.EchoPayloads;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class EchoSyncService {
    public static boolean sendPlayback(ServerPlayer viewer, ServerPlayer source, boolean forced) {
        if (viewer == source || viewer.level() != source.level()) return false;
        List<EchoFrame> frames = PlayerEchoRecorder.playback(source, viewer.server.getTickCount());
        if (frames.size() < 4) {
            debug("rejected source={} viewer={} reason=insufficient_frames count={}", source, viewer, frames.size());
            return false;
        }
        EchoPayloads.Playback payload = new EchoPayloads.Playback(UUID.randomUUID(), source.level().dimension().location(),
                EchoAppearanceSnapshot.capture(source), frames, 10, 20);
        PacketDistributor.sendToPlayer(viewer, payload);
        debug("sent source={} viewer={} frames={} forced={}", source, viewer, frames.size(), forced);
        return true;
    }

    private static void debug(String template, ServerPlayer source, ServerPlayer viewer, Object... values) {
        if (!EchoServerConfig.DEBUG.get()) return;
        Object[] arguments = new Object[values.length + 2];
        arguments[0] = source.getGameProfile().getName(); arguments[1] = viewer.getGameProfile().getName();
        System.arraycopy(values, 0, arguments, 2, values.length);
        MaplesAdventure.LOGGER.info("[MaplesAdventure/Echo] " + template, arguments);
    }
    private EchoSyncService() {}
}
