package dev.maplesadventure.bonfire;

import dev.maplesadventure.progression.ProgressionAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerRespawnPositionEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class BonfireEvents {
    public static void register() { NeoForge.EVENT_BUS.register(new BonfireEvents()); }
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
}
