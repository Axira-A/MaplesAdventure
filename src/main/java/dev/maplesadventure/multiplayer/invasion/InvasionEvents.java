package dev.maplesadventure.multiplayer.invasion;

import dev.maplesadventure.config.InvasionConfig;
import dev.maplesadventure.multiplayer.encounter.fog.FogTraversalManager;
import dev.maplesadventure.multiplayer.phase.PhaseManager;
import dev.maplesadventure.multiplayer.phase.PhaseRole;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;

public final class InvasionEvents {
    private static final Set<UUID> PENDING_RECOVERY = new HashSet<>();
    public static void register() {
        NeoForge.EVENT_BUS.register(new InvasionEvents());
        FogTraversalManager.setInvasionCoordinator(InvasionSessionManager.INSTANCE);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        InvasionSession session = InvasionSessionManager.session(player.getUUID()).orElse(null);
        if (session == null) return;
        if (session.invaderUuid().equals(player.getUUID())) {
            event.setCanceled(true);
            player.setHealth(Math.max(1.0F, player.getMaxHealth() * 0.25F));
            player.invulnerableTime = Math.max(player.invulnerableTime, 40);
            InvasionSessionManager.endForPlayer(player.getUUID(), InvasionSessionManager.EndReason.INVADER_DEATH);
        } else if (session.hostUuid().equals(player.getUUID())) {
            InvasionSessionManager.endForPlayer(player.getUUID(), InvasionSessionManager.EndReason.HOST_DEATH);
        }
    }

    @SubscribeEvent public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) PENDING_RECOVERY.add(player.getUUID());
    }
    @SubscribeEvent public void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && PENDING_RECOVERY.remove(player.getUUID())) {
            InvasionSessionManager.recoverPendingReturn(player);
        }
    }
    @SubscribeEvent public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PENDING_RECOVERY.remove(player.getUUID());
        InvasionQueueManager.remove(player.getUUID());
        InvasionSession session = InvasionSessionManager.session(player.getUUID()).orElse(null);
        if (session != null) InvasionSessionManager.endForPlayer(player.getUUID(),
                session.invaderUuid().equals(player.getUUID())
                        ? InvasionSessionManager.EndReason.INVADER_LOGOUT : InvasionSessionManager.EndReason.HOST_LOGOUT);
    }
    @SubscribeEvent public void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        InvasionQueueManager.remove(player.getUUID());
        InvasionSession session = InvasionSessionManager.session(player.getUUID()).orElse(null);
        if (session != null && (session.invaderUuid().equals(player.getUUID()) || session.hostUuid().equals(player.getUUID()))) {
            InvasionSessionManager.endForPlayer(player.getUUID(), InvasionSessionManager.EndReason.DIMENSION_CHANGE);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST) public void onBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && isInvader(player)) event.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && isInvader(player)) event.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player && isInvader(player)) event.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public void onUseWorldItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player && isInvader(player)
                && (event.getItemStack().getItem() instanceof BlockItem
                || event.getItemStack().getItem() instanceof BucketItem)) event.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public void onUseEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() instanceof ServerPlayer player && isInvader(player)) event.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public void onUseEntitySpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getEntity() instanceof ServerPlayer player && isInvader(player)) event.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public void onToss(ItemTossEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && isInvader(player)) {
            // NeoForge documents that cancellation alone removes the stack from the inventory.
            // Put the exact server-owned stack back before preventing its world entity from spawning.
            player.getInventory().placeItemBackInInventory(event.getEntity().getItem().copy());
            event.setCanceled(true);
        }
    }

    @SubscribeEvent public void onServerTick(ServerTickEvent.Post event) {
        InvasionSessionManager.tick(event.getServer());
        if (InvasionConfig.ENABLED.get() && event.getServer().getTickCount() % InvasionConfig.MATCH_INTERVAL_TICKS.get() == 0)
            InvasionQueueManager.tick(event.getServer());
    }
    @SubscribeEvent public void onStopping(ServerStoppingEvent event) {
        for (InvasionSession session : InvasionSessionManager.activeSessions())
            InvasionSessionManager.endForPlayer(session.hostUuid(), InvasionSessionManager.EndReason.SERVER_STOPPING);
        InvasionQueueManager.clear();
    }
    @SubscribeEvent public void onStopped(ServerStoppedEvent event) {
        PENDING_RECOVERY.clear(); InvasionSessionManager.clearTransient(); InvasionQueueManager.clear();
    }
    private static boolean isInvader(ServerPlayer player) {
        return PhaseManager.state(player).role() == PhaseRole.INVADER
                && InvasionSessionManager.hasSession(player.getUUID());
    }
    private InvasionEvents() {}
}
