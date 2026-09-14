package dev.maplesadventure.multiplayer.echo.client;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.multiplayer.echo.EpicFightEchoFrameState;
import dev.maplesadventure.multiplayer.echo.network.EchoPayloads;
import java.lang.reflect.InvocationTargetException;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;

/** Optional classloading boundary: this class contains no Epic Fight types. */
public final class EpicFightEchoClientBridge {
    private static final String ADAPTER =
            "dev.maplesadventure.integration.epicfight.echo.EpicFightEchoClientAdapter";
    private static CaptureAdapter adapter;
    private static boolean initialized;
    private static boolean warned;

    public static void sampleAndSend() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getConnection() == null || minecraft.player.tickCount % 2 != 0) return;
        CaptureAdapter capture = adapter();
        if (capture == null) return;
        try {
            PacketDistributor.sendToServer(new EchoPayloads.AnimationSample(capture.capture()));
        } catch (RuntimeException exception) {
            warnOnce("Epic Fight echo animation capture failed; using vanilla echo poses", exception);
        }
    }

    private static CaptureAdapter adapter() {
        if (initialized) return adapter;
        initialized = true;
        if (!ModList.get().isLoaded("epicfight")) return null;
        try {
            adapter = (CaptureAdapter) Class.forName(ADAPTER).getConstructor().newInstance();
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException
                 | IllegalAccessException | InvocationTargetException | LinkageError exception) {
            warnOnce("Epic Fight echo adapter is unavailable; using vanilla echo poses", exception);
        }
        return adapter;
    }

    private static void warnOnce(String message, Throwable error) {
        if (!warned) {
            warned = true;
            MaplesAdventure.LOGGER.warn(message, error);
        }
    }

    public interface CaptureAdapter { EpicFightEchoFrameState capture(); }
    private EpicFightEchoClientBridge() {}
}
