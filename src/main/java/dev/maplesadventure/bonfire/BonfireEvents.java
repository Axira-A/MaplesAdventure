package dev.maplesadventure.bonfire;

import dev.maplesadventure.progression.ProgressionAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerRespawnPositionEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class BonfireEvents {
    public static void register() { NeoForge.EVENT_BUS.register(new BonfireEvents()); }
    @SubscribeEvent public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) BonfireStateService.sync(player);
    }
    @SubscribeEvent public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) BonfireStateService.sync(player);
    }
    @SubscribeEvent public void onRespawnPosition(PlayerRespawnPositionEvent event) {
        if (event.isFromEndFight()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        player.getExistingData(ProgressionAttachments.PLAYER_BONFIRES)
                .flatMap(PlayerBonfireState::lastRested)
                .flatMap(point -> BonfireRespawnResolver.resolve(player.server, player, point))
                .ifPresent(event::setDimensionTransition);
    }
    @SubscribeEvent public void onClone(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer player
                && player.getExistingData(ProgressionAttachments.PLAYER_BONFIRES).isEmpty())
            event.getOriginal().getExistingData(ProgressionAttachments.PLAYER_BONFIRES).ifPresent(old -> {
                PlayerBonfireState copy = new PlayerBonfireState();
                copy.deserializeNBT(player.registryAccess(), old.serializeNBT(player.registryAccess()));
                player.setData(ProgressionAttachments.PLAYER_BONFIRES, copy);
            });
    }
    @SubscribeEvent public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) BonfireSessionService.close(player);
    }
    @SubscribeEvent public void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) BonfireSessionService.close(player);
    }
    @SubscribeEvent public void onDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) BonfireSessionService.close(player);
    }
    @SubscribeEvent public void onTick(ServerTickEvent.Post event) { BonfireSessionService.tick(event.getServer()); }
    @SubscribeEvent public void onStopped(ServerStoppedEvent event) { BonfireSessionService.clear(); }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public void onAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && BonfireSessionService.isBusy(player)) event.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public void onDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer target && BonfireSessionService.isBusy(target)) {
            event.setCanceled(true);
            return;
        }
        if (event.getSource().getEntity() instanceof ServerPlayer player
                && event.getSource().getDirectEntity() == player && BonfireSessionService.isBusy(player)) event.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public void onInvulnerability(
            net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent event) {
        if (BonfirePoseLock.locked(event.getEntity())) event.setInvulnerable(true);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public void onKnockback(
            net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent event) {
        if (BonfirePoseLock.locked(event.getEntity())) event.setCanceled(true);
    }
    @SubscribeEvent public void onPlayerPre(net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre event) {
        if (BonfirePoseLock.locked(event.getEntity())) BonfirePoseLock.stopMotion(event.getEntity());
    }
    @SubscribeEvent public void onPlayerPost(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (BonfirePoseLock.locked(event.getEntity())) BonfirePoseLock.stopMotion(event.getEntity());
    }
    private void blockBusyInteraction(PlayerInteractEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && BonfireSessionService.isBusy(player)
                && event instanceof ICancellableEvent cancel) cancel.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public void onRightBlock(PlayerInteractEvent.RightClickBlock event) { blockBusyInteraction(event); }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public void onRightItem(PlayerInteractEvent.RightClickItem event) { blockBusyInteraction(event); }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public void onEntity(PlayerInteractEvent.EntityInteract event) { blockBusyInteraction(event); }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public void onEntitySpecific(PlayerInteractEvent.EntityInteractSpecific event) { blockBusyInteraction(event); }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public void onLeftBlock(PlayerInteractEvent.LeftClickBlock event) { blockBusyInteraction(event); }
}
