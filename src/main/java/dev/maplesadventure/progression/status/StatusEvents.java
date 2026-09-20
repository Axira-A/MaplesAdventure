package dev.maplesadventure.progression.status;

import net.neoforged.bus.api.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.*;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import dev.maplesadventure.progression.ProgressionAttachments;
import dev.maplesadventure.progression.weapon.WeaponDamagePolicy;

public final class StatusEvents {
    public static void register() { NeoForge.EVENT_BUS.register(new StatusEvents()); }
    @SubscribeEvent(priority=EventPriority.LOWEST) public void post(LivingDamageEvent.Post e) {
        if(e.getEntity().level().isClientSide()) return;
        var hit=dev.maplesadventure.progression.defense.CombatHitLifecycle.consume(e.getEntity().getUUID(),e.getSource());
        if(!StatusGuardPolicy.permitsBuildup(e.getNewDamage())||StatusDamageSources.isStatus(e.getSource())) return;
        StatusControlLockService.wakeOnHit(e.getEntity());
        if(e.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FIRE)
                || hit.filter(dev.maplesadventure.progression.defense.CombatHitLifecycle::hasFire).isPresent()) StatusRuntimeService.fireHit(e.getEntity());
        hit.ifPresent(context->context.statuses().amounts().forEach((type,amount)->
                StatusBuildupService.apply(e.getEntity(),type,amount,new StatusSourceContext(context.owner(),StatusSourceContext.SourceKind.WEAPON,
                        context.usedWeapon(),context.projectileSnapshot(),type))));
    }
    @SubscribeEvent public void tick(ServerTickEvent.Post e) {
        StatusRuntimeService.tick(e.getServer());
        dev.maplesadventure.progression.defense.CombatHitLifecycle.clear();
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void damageLock(LivingIncomingDamageEvent e) {
        if(!StatusDamageSources.isStatus(e.getSource())&&e.getSource().getEntity() instanceof LivingEntity source&&StatusControlLockService.locked(source)
                &&e.getSource().getDirectEntity()==source) e.setCanceled(true);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void attackLock(net.neoforged.neoforge.event.entity.player.AttackEntityEvent e) {
        if(StatusControlLockService.locked(e.getEntity())) e.setCanceled(true);
    }
    private void useLock(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent e) {
        if(e instanceof net.neoforged.bus.api.ICancellableEvent cancel&&StatusControlLockService.locked(e.getEntity())) cancel.setCanceled(true);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void rightBlock(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock e) { useLock(e); }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void rightItem(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem e) { useLock(e); }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void entityUse(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract e) { useLock(e); }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void entityUseSpecific(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteractSpecific e) { useLock(e); }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void leftBlock(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock e) { useLock(e); }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void useStart(LivingEntityUseItemEvent.Start e) {
        if(StatusControlLockService.locked(e.getEntity())) e.setCanceled(true);
    }
    @SubscribeEvent public void join(EntityJoinLevelEvent e) {
        if(!e.getLevel().isClientSide()&&e.getEntity() instanceof LivingEntity living) StatusRuntimeService.track(living);
    }
    @SubscribeEvent public void leave(EntityLeaveLevelEvent e) {
        if(!e.getLevel().isClientSide()&&e.getEntity() instanceof LivingEntity living) StatusRuntimeService.forget(living);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST,receiveCanceled=true) public void death(LivingDeathEvent e) {
        // Phantom death is cancelled by the established session handlers, but still ends this life.
        if(!e.isCanceled()||e.getEntity() instanceof ServerPlayer) StatusRuntimeService.clearAll(e.getEntity(),StatusRuntimeService.ClearReason.DEATH);
    }
    @SubscribeEvent public void clone(PlayerEvent.Clone e) {
        if(!e.isWasDeath()) e.getOriginal().getExistingData(ProgressionAttachments.STATUS_RUNTIME).ifPresent(old->{
            var copy=new StatusRuntimeState(); copy.deserializeNBT(null,old.serializeNBT(null)); e.getEntity().setData(ProgressionAttachments.STATUS_RUNTIME,copy);
        });
        else e.getEntity().removeData(ProgressionAttachments.STATUS_RUNTIME);
    }
    @SubscribeEvent public void login(PlayerEvent.PlayerLoggedInEvent e) { if(e.getEntity() instanceof ServerPlayer p) { StatusRuntimeService.track(p); StatusNetwork.sync(p); } }
    @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent e) { StatusRuntimeService.forget(e.getEntity()); }
    @SubscribeEvent public void respawn(PlayerEvent.PlayerRespawnEvent e) { if(e.getEntity() instanceof ServerPlayer p) { StatusRuntimeService.track(p); StatusNetwork.sync(p); } }
    @SubscribeEvent public void dimension(PlayerEvent.PlayerChangedDimensionEvent e) { if(e.getEntity() instanceof ServerPlayer p) { StatusRuntimeService.track(p); StatusNetwork.sync(p); } }
    @SubscribeEvent public void stop(ServerStoppedEvent e) { StatusRuntimeService.shutdown(); dev.maplesadventure.progression.defense.CombatHitLifecycle.clear(); }
}
