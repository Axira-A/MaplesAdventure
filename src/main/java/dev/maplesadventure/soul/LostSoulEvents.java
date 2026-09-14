package dev.maplesadventure.soul;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.config.LostSoulConfig;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Death is captured once here; Clone and Respawn only enforce the already-created transaction. */
public final class LostSoulEvents {
    public static void register() {
        NeoForge.EVENT_BUS.register(new LostSoulEvents());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void capturePlayerDeathExperience(LivingDeathEvent event) {
        if (!event.isCanceled() && LostSoulConfig.ENABLED.get() && event.getEntity() instanceof ServerPlayer player) {
            var role = dev.maplesadventure.multiplayer.phase.PhaseManager.state(player).role();
            if (role == dev.maplesadventure.multiplayer.phase.PhaseRole.COOPERATOR
                    || role == dev.maplesadventure.multiplayer.phase.PhaseRole.INVADER) return;
            LostSoulManager.captureDeathExperience(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        var role = dev.maplesadventure.multiplayer.phase.PhaseManager.state(player).role();
        if (role == dev.maplesadventure.multiplayer.phase.PhaseRole.COOPERATOR
                || role == dev.maplesadventure.multiplayer.phase.PhaseRole.INVADER) {
            LostSoulManager.discardCapturedDeathExperience(player.getUUID());
            return;
        }
        if (event.isCanceled() || !LostSoulConfig.ENABLED.get()) {
            LostSoulManager.discardCapturedDeathExperience(player.getUUID());
            return;
        }
        if (LostSoulManager.hasPendingReset(player.getUUID())) {
            LostSoulManager.discardCapturedDeathExperience(player.getUUID());
            MaplesAdventure.LOGGER.warn("Ignored duplicate death event for {} before respawn", player.getGameProfile().getName());
            return;
        }
        int totalExperience = LostSoulManager.consumeCapturedDeathExperience(player);
        if (LostSoulManager.createSoulForDeath(player, totalExperience)) {
            ExperiencePoints.setExact(player, 0);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onDeathExperienceDrop(LivingExperienceDropEvent event) {
        if (LostSoulConfig.ENABLED.get() && event.getEntity() instanceof ServerPlayer) {
            if (event.getEntity() instanceof ServerPlayer player) {
                var role = dev.maplesadventure.multiplayer.phase.PhaseManager.state(player).role();
                if (role == dev.maplesadventure.multiplayer.phase.PhaseRole.COOPERATOR
                        || role == dev.maplesadventure.multiplayer.phase.PhaseRole.INVADER) return;
            }
            event.setDroppedExperience(0);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath() && event.getEntity() instanceof ServerPlayer player
                && LostSoulManager.hasPendingReset(player.getUUID())) {
            ExperiencePoints.setExact(player, 0);
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && LostSoulManager.consumePendingReset(player.getUUID())) {
            ExperiencePoints.setExact(player, 0);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LostSoulManager.recordSafePosition(player);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        LostSoulManager.forgetPlayer(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        LostSoulManager.clearTransientState();
    }

    private LostSoulEvents() {
    }
}
