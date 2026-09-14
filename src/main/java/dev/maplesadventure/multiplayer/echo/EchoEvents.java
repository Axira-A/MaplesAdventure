package dev.maplesadventure.multiplayer.echo;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Keeps transient trajectories segmented across all discontinuous player lifecycle transitions. */
public final class EchoEvents {
    public static void register() { NeoForge.EVENT_BUS.register(new EchoEvents()); }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        PlayerEchoRecorder.sample(event.getServer());
        EchoScheduler.tick(event.getServer());
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) PlayerEchoRecorder.cut(player.getUUID());
    }

    @SubscribeEvent
    public void onClone(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer player) PlayerEchoRecorder.cut(player.getUUID());
    }

    @SubscribeEvent
    public void onDimensionChanged(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) PlayerEchoRecorder.cut(player.getUUID());
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerEchoRecorder.cut(player.getUUID());
            EchoScheduler.forget(player.getUUID());
        }
    }

    @SubscribeEvent
    public void onStopped(ServerStoppedEvent event) {
        PlayerEchoRecorder.clear();
        EchoScheduler.clear();
    }

    private EchoEvents() {}
}
