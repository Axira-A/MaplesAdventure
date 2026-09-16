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
        if(e.getEntity().level().isClientSide()||e.getNewDamage()<=0||StatusDamageSources.isStatus(e.getSource())) return;
        if(e.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) StatusRuntimeService.fireHit(e.getEntity());
        WeaponDamagePolicy.context(e.getSource()).ifPresent(context->context.statuses().amounts().forEach((type,amount)->
                StatusBuildupService.apply(e.getEntity(),type,amount,new StatusSourceContext(context.owner(),StatusSourceContext.SourceKind.WEAPON,
                        context.usedWeapon(),context.projectileSnapshot(),type))));
    }
    @SubscribeEvent public void tick(ServerTickEvent.Post e) { StatusRuntimeService.tick(e.getServer()); }
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
    @SubscribeEvent public void stop(ServerStoppedEvent e) { StatusRuntimeService.shutdown(); }
}
