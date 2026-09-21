package dev.maplesadventure.progression.status;

import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import dev.maplesadventure.progression.ProgressionAttachments;

public final class StatusRuntimeService {
    public enum ClearReason { ADMIN, DEATH, EXPIRE, FIRE_RESET, CURE, SESSION_RETURN }
    private static final Set<LivingEntity> ACTIVE=Collections.newSetFromMap(new IdentityHashMap<>());
    public static long now(LivingEntity target) { return target.getServer().overworld().getGameTime(); }
    public static StatusRuntimeState state(LivingEntity target) { return target.getData(ProgressionAttachments.STATUS_RUNTIME); }
    public static boolean active(LivingEntity target,StatusEffectType type) {
        if(target.level().isClientSide()) return false;
        var state=target.getExistingData(ProgressionAttachments.STATUS_RUNTIME).orElse(null);
        var e=state==null?null:state.get(type); return e!=null&&e.active(now(target));
    }
    public static double frostRegen(LivingEntity target) { return active(target,StatusEffectType.FROSTBITE)?StatusDefinitions.get(StatusEffectType.FROSTBITE).staminaRegenMultiplier():1; }
    public static double damageTaken(LivingEntity target) { return active(target,StatusEffectType.FROSTBITE)?StatusDefinitions.get(StatusEffectType.FROSTBITE).damageTakenMultiplier():1; }
    public static void track(LivingEntity target) {
        var state=target.getExistingData(ProgressionAttachments.STATUS_RUNTIME).orElse(null);
        if(state!=null&&!state.empty()) {
            // No accumulated offline DOT catch-up. Persistent duration still elapses in server game time.
            long now=now(target); for(var e:state.entries().values()) if(e.nextDot<now) e.nextDot=now+1;
            ACTIVE.add(target);
        }
    }
    public static void forget(LivingEntity target) { ACTIVE.remove(target); }
    public static void shutdown() { ACTIVE.clear(); StatusDefinitions.reset(); StatusResistanceCorrections.reset(); StatusMotionValueResolver.clear(); WeaponStatusRules.clear(); }
    static void changed(LivingEntity target) {
        if(state(target).empty()) ACTIVE.remove(target); else ACTIVE.add(target);
        if(target instanceof ServerPlayer player) {
            StatusNetwork.sync(player);
            dev.maplesadventure.progression.encumbrance.EncumbranceRuntimeService.equipmentChanged(player);
        }
    }
    public static void clear(LivingEntity target,StatusEffectType type,ClearReason reason) {
        if(target.level().isClientSide()||dev.maplesadventure.integration.api.ApiNotifications.busy()) return;
        var state=target.getExistingData(ProgressionAttachments.STATUS_RUNTIME).orElse(null);
        if(state==null) return;
        boolean affected=state.get(type)!=null||state.lockType==type||reason==ClearReason.ADMIN&&state.procCount(type)>0;
        if(!affected) return;
        var before=dev.maplesadventure.integration.api.StatusEventPublisher.view(target,type);
        state.remove(type); changed(target);
        if(state.lockType==type&&reason!=ClearReason.EXPIRE) { state.unlock(); changed(target); }
        if(reason==ClearReason.ADMIN) state.resetCorrection(type);
        dev.maplesadventure.integration.api.StatusEventPublisher.cleared(target,before,dev.maplesadventure.api.event.StatusClearEvent.Reason.valueOf(reason.name()));
    }
    /** Clears only pending accumulation; does not cure an active effect or forget correction. */
    public static void clearBuildup(LivingEntity target,StatusEffectType type) {
        if(target.level().isClientSide()||dev.maplesadventure.integration.api.ApiNotifications.busy()) return;
        var state=target.getExistingData(ProgressionAttachments.STATUS_RUNTIME).orElse(null);
        var entry=state==null?null:state.get(type);
        if(entry==null||entry.current==0) return;
        var before=dev.maplesadventure.integration.api.StatusEventPublisher.view(target,type);
        entry.current=0; state.changed(); changed(target);
        dev.maplesadventure.integration.api.StatusEventPublisher.cleared(target,before,dev.maplesadventure.api.event.StatusClearEvent.Reason.BUILDUP_ONLY);
    }
    public static void clearAll(LivingEntity target,ClearReason reason) {
        if(target.level().isClientSide()||dev.maplesadventure.integration.api.ApiNotifications.busy()) return;
        var state=target.getExistingData(ProgressionAttachments.STATUS_RUNTIME).orElse(null);
        var before=new java.util.ArrayList<dev.maplesadventure.api.status.StatusView>();
        if(state!=null) for(var type:StatusEffectType.values())
            if(state.get(type)!=null||state.lockType==type||state.procCount(type)>0)
                before.add(dev.maplesadventure.integration.api.StatusEventPublisher.view(target,type));
        if(state!=null) state.clear();
        ACTIVE.remove(target);
        if(target instanceof ServerPlayer p) { StatusNetwork.sync(p); dev.maplesadventure.progression.encumbrance.EncumbranceRuntimeService.equipmentChanged(p); }
        for(var view:before) dev.maplesadventure.integration.api.StatusEventPublisher.cleared(target,view,dev.maplesadventure.api.event.StatusClearEvent.Reason.valueOf(reason.name()));
    }
    public static void fireHit(LivingEntity target) {
        if(active(target,StatusEffectType.FROSTBITE)) clear(target,StatusEffectType.FROSTBITE,ClearReason.FIRE_RESET);
    }
    static void proc(LivingEntity target,StatusEffectType type,StatusResistance resistance) {
        var state=state(target); var entry=state.get(type); if(entry==null) return;
        var definition=StatusDefinitions.get(type); long now=now(target);
        if(target instanceof ServerPlayer player) StatusNetwork.proc(player,type,state.revision(),entry.procSerial);
        if(type==StatusEffectType.BLEED||type==StatusEffectType.FROSTBITE||type==StatusEffectType.MADNESS) damage(target,type,entry,definition,resistance);
        if(type==StatusEffectType.DEATH_BLIGHT) target.hurt(StatusDamageSources.create(target,type,entry.source),Float.MAX_VALUE);
        // A death (including phantom death/return) may have cleared the container during hurt.
        if(!target.isAlive()||state.get(type)!=entry) return;
        if(type==StatusEffectType.SLEEP||type==StatusEffectType.MADNESS) {
            if(target instanceof ServerPlayer player) dev.maplesadventure.progression.runtime.PlayerManaService.consumeFraction(player,definition.manaFlat(),definition.manaFraction());
            boolean deep=type==StatusEffectType.SLEEP&&!(target instanceof net.minecraft.world.entity.player.Player)&&resistance.sleepResponse()==SleepResponse.DEEP_SLEEP;
            int duration=deep?definition.deepSleepTicks():definition.controlTicks();
            if(duration>0) StatusControlLockService.lock(target,type,duration,deep);
        }
        if(definition.duration()>0) {
            entry.activeStart=now; entry.activeEnd=now+definition.duration(); entry.nextDot=now+definition.tickInterval();
        }
        state.changed(); changed(target);
    }
    private static void damage(LivingEntity target,StatusEffectType type,StatusRuntimeState.Entry entry,StatusEffectDefinition definition,StatusResistance resistance) {
        double amount=definition.damage(target.getMaxHealth(),resistance.procDamageMultiplier());
        if(amount>0) target.hurt(StatusDamageSources.create(target,type,entry.source),(float)Math.min(amount,Float.MAX_VALUE));
    }
    public static void tick(MinecraftServer server) {
        if(server.getTickCount()%5!=0) return;
        for(var target:List.copyOf(ACTIVE)) {
            if(target.isRemoved()||!target.isAlive()) { ACTIVE.remove(target); continue; }
            long now=now(target); var state=state(target);
            StatusControlLockService.tick(target,now,state);
            for(var type:List.copyOf(state.entries().keySet())) {
                var e=state.get(type); if(e==null) continue; var d=StatusDefinitions.get(type);
                if(e.hasDuration()) {
                    // Include the final scheduled pulse at activeEnd. At most one pulse per
                    // server check; no offline catch-up, and no expiration-before-last-pulse loss.
                    if((type==StatusEffectType.POISON||type==StatusEffectType.SCARLET_ROT)&&now>=e.nextDot&&e.nextDot<=e.activeEnd) {
                        e.nextDot+=d.tickInterval(); damage(target,type,e,d,StatusResistanceService.resolve(target,type));
                    }
                    if(state.get(type)==e&&!e.active(now)) clear(target,type,ClearReason.EXPIRE);
                } else {
                    e.decay(now,d);
                    if(e.current==0) clear(target,type,ClearReason.EXPIRE);
                }
            }
        }
    }
    private StatusRuntimeService() {}
}
