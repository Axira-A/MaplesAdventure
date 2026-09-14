package dev.maplesadventure.multiplayer.phase;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.config.PhaseConfig;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/** Server authority and lifecycle hooks for phase-one player isolation. */
public final class PhaseEvents {
    public static void register() { NeoForge.EVENT_BUS.register(new PhaseEvents()); }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            PhaseManager.assignSolo(player);
            PhaseSyncService.login(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            PhaseSyncService.remove(player.getUUID());
            PhaseManager.forget(player.getUUID());
        }
    }

    @SubscribeEvent
    public void onClone(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            PhaseManager.state(player);
            PhaseSyncService.sendSnapshot(player);
        }
    }

    @SubscribeEvent
    public void onDimensionChanged(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            PhaseSyncService.sendSnapshot(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        Entity target = event.getEntity();
        if (target.level().isClientSide()) return;
        Entity attacker = event.getSource().getEntity();
        if (attacker == null) attacker = event.getSource().getDirectEntity();
        if (target instanceof Player player
                && dev.maplesadventure.multiplayer.invasion.InvasionSessionManager.isMaterializing(player.getUUID())) {
            event.setCanceled(true);
            return;
        }
        if (attacker != null && !PhaseRelations.canDamage(attacker, target)) {
            event.setCanceled(true);
            debugBlocked("DAMAGE", attacker, target);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (rejectInteraction(event.getEntity(), event.getTarget())) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (rejectInteraction(event.getEntity(), event.getTarget())) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

    private static boolean rejectInteraction(Player actor, Entity target) {
        if (!PhaseRelations.canInteract(actor, target)) {
            if (!actor.level().isClientSide()) debugBlocked("INTERACT", actor, target);
            return true;
        }
        return false;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getRayTraceResult() instanceof EntityHitResult hit)) return;
        Entity target = hit.getEntity();
        Entity owner = event.getProjectile().getOwner();
        if (owner != null && !PhaseRelations.canDamage(owner, target)) {
            event.setCanceled(true);
            if (!event.getProjectile().level().isClientSide()) debugBlocked("PROJECTILE", owner, target);
        }
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) { PhaseManager.clear(); }

    static void debugBlocked(String operation, Entity actor, Entity target) {
        if (!PhaseConfig.DEBUG.get()) return;
        String actorPhase = PhaseMembership.phaseOf(actor).map(Object::toString).orElse("shared");
        String targetPhase = PhaseMembership.phaseOf(target).map(Object::toString).orElse("shared");
        MaplesAdventure.LOGGER.info("[MaplesAdventure/Phase] blocked {} source={} target={} phaseA={} phaseB={}",
                operation, actor.getName().getString(), target.getName().getString(), actorPhase, targetPhase);
    }

    private PhaseEvents() {}
}
