package dev.maplesadventure.bonfire;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.integration.epicfight.EpicFightIntegration;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;

/** Optional visual bridge. A failed or absent animation never changes a rest transaction. */
public final class BonfireAnimationIntegration {
    public interface Adapter {
        void register(IEventBus modBus);
        void play(ServerPlayer player, BonfireSessionState state);
        void stop(ServerPlayer player);
    }

    private static Adapter adapter;
    private static boolean warned;

    public static void register(IEventBus modBus) {
        if (!EpicFightIntegration.isLoaded()) return;
        try {
            adapter = (Adapter) Class.forName(
                    "dev.maplesadventure.integration.epicfight.bonfire.EpicFightBonfireAnimationAdapter")
                    .getDeclaredConstructor().newInstance();
            adapter.register(modBus);
        } catch (ReflectiveOperationException | LinkageError error) {
            warn(error);
            adapter = null;
        }
    }

    public static void play(ServerPlayer player, BonfireSessionState state) {
        if (adapter == null) return;
        try { adapter.play(player, state); }
        catch (RuntimeException | LinkageError error) { warn(error); }
    }

    public static void stop(ServerPlayer player) {
        if (adapter == null) return;
        try { adapter.stop(player); }
        catch (RuntimeException | LinkageError error) { warn(error); }
    }

    private static void warn(Throwable error) {
        if (warned) return;
        warned = true;
        MaplesAdventure.LOGGER.warn("Bonfire animation is unavailable; rest gameplay remains active", error);
    }

    private BonfireAnimationIntegration() {}
}
