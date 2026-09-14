package dev.maplesadventure.multiplayer.coop;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import dev.maplesadventure.multiplayer.phase.PhaseManager;
import dev.maplesadventure.multiplayer.phase.PhaseRole;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class CoopEvents {
    private static final Set<UUID> PENDING_LOGIN_RECOVERY = new HashSet<>();

    public static void register() { NeoForge.EVENT_BUS.register(new CoopEvents()); }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PhaseRole role = PhaseManager.state(player).role();
        SummonSignManager.removeOwner(player.server, player.getUUID());
        if (role == PhaseRole.COOPERATOR && CoopSessionManager.hasSession(player.getUUID())) {
            event.setCanceled(true);
            player.setHealth(Math.max(1.0F, player.getMaxHealth() * 0.25F));
            player.invulnerableTime = Math.max(player.invulnerableTime, 40);
            CoopSessionManager.endForPlayer(player.getUUID(), CoopSessionManager.EndReason.COOPERATOR_DEATH);
        } else if (role == PhaseRole.HOST && CoopSessionManager.hasSession(player.getUUID())) {
            CoopSessionManager.endForPlayer(player.getUUID(), CoopSessionManager.EndReason.HOST_DEATH);
        }
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // PlayerLoggedInEvent fires while the player is still being attached to the level.
            // Defer the persistent return by one player tick so cross-dimension teleport and
            // phase snapshots cannot be overwritten by the tail of Vanilla's login pipeline.
            PENDING_LOGIN_RECOVERY.add(player.getUUID());
            SummonSignSyncService.syncNow(player);
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PENDING_LOGIN_RECOVERY.remove(player.getUUID());
        CoopSession session = CoopSessionManager.session(player.getUUID()).orElse(null);
        if (session != null) {
            CoopSessionManager.EndReason reason = session.hostUuid().equals(player.getUUID())
                    ? CoopSessionManager.EndReason.HOST_LOGOUT
                    : CoopSessionManager.EndReason.COOPERATOR_LOGOUT;
            CoopSessionManager.endForPlayer(player.getUUID(), reason);
        }
        SummonSignManager.removeOwner(player.server, player.getUUID());
        SummonSignSyncService.forget(player.getUUID());
    }

    @SubscribeEvent
    public void onDimensionChanged(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (CoopSessionManager.hasSession(player.getUUID())) {
            CoopSessionManager.endForPlayer(player.getUUID(), CoopSessionManager.EndReason.DIMENSION_CHANGE);
        }
        SummonSignManager.removeOwner(player.server, player.getUUID());
        SummonSignSyncService.syncNow(player);
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (PENDING_LOGIN_RECOVERY.remove(player.getUUID())) {
                CoopSessionManager.recoverPendingReturn(player);
            }
            SummonSignSyncService.tick(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player
                && PhaseManager.state(player).role() == PhaseRole.COOPERATOR) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && PhaseManager.state(player).role() == PhaseRole.COOPERATOR) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if ((event.getServer().getTickCount() % 20) == 0) SummonSignManager.maintenance(event.getServer());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        SummonSignManager.clear();
        CoopSessionManager.clearTransient();
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        PENDING_LOGIN_RECOVERY.clear();
        SummonSignSyncService.clear();
    }

    private CoopEvents() {}
}
