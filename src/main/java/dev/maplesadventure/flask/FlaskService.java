package dev.maplesadventure.flask;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.api.flask.*;
import dev.maplesadventure.progression.ProgressionAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

/** All authoritative state mutations run on the server thread; commands use the same invariants. */
public final class FlaskService {
    public static void checkThread(ServerPlayer player) {
        if (!player.server.isSameThread()) throw new IllegalStateException("Flask API requires server thread");
    }
    public static FlaskSnapshot state(ServerPlayer player) {
        checkThread(player);
        FlaskState stored = player.getData(ProgressionAttachments.FLASK);
        FlaskSnapshot old = stored.value();
        FlaskSnapshot normalized = stored.initialized()
                ? FlaskState.normalize(old.totalCapacity(), old.potencyLevel(), old.crimsonAllocated(), old.crimsonRemaining(),
                    old.ashenAllocated(), old.ashenRemaining(), FlaskManaBridge.available())
                : FlaskState.initial(FlaskManaBridge.available());
        if (!stored.initialized() || !old.equals(normalized)) player.setData(ProgressionAttachments.FLASK, new FlaskState(normalized));
        return normalized;
    }
    static void write(ServerPlayer player, FlaskSnapshot next) {
        checkThread(player);
        player.setData(ProgressionAttachments.FLASK, new FlaskState(FlaskState.normalize(next.totalCapacity(), next.potencyLevel(),
                next.crimsonAllocated(), next.crimsonRemaining(), next.ashenAllocated(), next.ashenRemaining(), FlaskManaBridge.available())));
    }
    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new FlaskPayloads.Snapshot(state(player), FlaskManaBridge.available(), FlaskUseController.kindId(player)));
    }
    public static void refill(ServerPlayer player, FlaskRechargeReason reason) {
        FlaskUseController.cancel(player, FlaskCancelReason.BONFIRE);
        write(player, state(player).refill()); sync(player);
        post(new FlaskEvents.Refill(player, reason));
    }
    /** Returns false if an extension throws; cancellable actions fail closed. Notifications never retry commits. */
    public static boolean post(Event event) {
        try { NeoForge.EVENT_BUS.post(event); return true; }
        catch (RuntimeException | LinkageError error) { MaplesAdventure.LOGGER.error("Flask extension event failed: {}", event.getClass().getSimpleName(), error); return false; }
    }
    public static boolean allocate(ServerPlayer player, int red, int blue) {
        FlaskSnapshot old = state(player);
        if (red < 0 || blue < 0 || red > old.totalCapacity() || blue > old.totalCapacity()
                || red + blue != old.totalCapacity() || (!FlaskManaBridge.available() && blue != 0)) return false;
        write(player, new FlaskSnapshot(old.totalCapacity(), old.potencyLevel(), red, red, blue, blue)); sync(player); return true;
    }
    /** OP commands only. Not exposed by FlaskApi or C2S. */
    public static void debugSet(ServerPlayer player, int capacity, int potency) {
        FlaskUseController.cancel(player, FlaskCancelReason.SCRIPTED);
        FlaskSnapshot old = state(player);
        int blue = Math.min(old.ashenAllocated(), Math.clamp(capacity, 4, 14));
        write(player, FlaskState.normalize(capacity, potency, capacity - blue, capacity - blue, blue, blue, FlaskManaBridge.available())); sync(player);
    }
    public static void debugReset(ServerPlayer player) {
        FlaskUseController.cancel(player, FlaskCancelReason.SCRIPTED);
        write(player, FlaskState.initial(FlaskManaBridge.available())); sync(player);
    }
    private FlaskService() {}
}
