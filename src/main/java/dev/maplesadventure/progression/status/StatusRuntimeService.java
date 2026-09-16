package dev.maplesadventure.progression.status;

import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import dev.maplesadventure.progression.ProgressionAttachments;

public final class StatusRuntimeService {
    public enum ClearReason { ADMIN, DEATH, EXPIRE, FIRE_RESET, CURE }
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
    public static void shutdown() { ACTIVE.clear(); StatusDefinitions.reset(); WeaponStatusRules.clear(); }
    static void changed(LivingEntity target) {
        if(state(target).empty()) ACTIVE.remove(target); else ACTIVE.add(target);
        if(target instanceof ServerPlayer player) {
            StatusNetwork.sync(player);
            dev.maplesadventure.progression.encumbrance.EncumbranceRuntimeService.equipmentChanged(player);
        }
    }
    public static void clear(LivingEntity target,StatusEffectType type,ClearReason reason) {
        if(target.level().isClientSide()) return;
        var state=target.getExistingData(ProgressionAttachments.STATUS_RUNTIME).orElse(null);
        if(state==null||state.get(type)==null) return;
        state.remove(type); changed(target);
    }
    public static void clearAll(LivingEntity target,ClearReason reason) {
        if(target.level().isClientSide()) return;
        var state=target.getExistingData(ProgressionAttachments.STATUS_RUNTIME).orElse(null);
        if(state!=null) state.clear();
        ACTIVE.remove(target);
        if(target instanceof ServerPlayer p) { StatusNetwork.sync(p); dev.maplesadventure.progression.encumbrance.EncumbranceRuntimeService.equipmentChanged(p); }
    }
    public static void fireHit(LivingEntity target) {
        if(active(target,StatusEffectType.FROSTBITE)) clear(target,StatusEffectType.FROSTBITE,ClearReason.FIRE_RESET);
    }
    static void proc(LivingEntity target,StatusEffectType type,StatusResistance resistance) {
        var state=state(target); var entry=state.get(type); if(entry==null) return;
        var definition=StatusDefinitions.get(type); long now=now(target);
        if(target instanceof ServerPlayer player) StatusNetwork.proc(player,type,state.revision(),entry.procSerial);
        if(type==StatusEffectType.BLEED||type==StatusEffectType.FROSTBITE) damage(target,type,entry,definition,resistance);
        // A death (including phantom death/return) may have cleared the container during hurt.
        if(!target.isAlive()||state.get(type)!=entry) return;
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
            for(var type:List.copyOf(state.entries().keySet())) {
                var e=state.get(type); if(e==null) continue; var d=StatusDefinitions.get(type);
                if(e.hasDuration()) {
                    if(!e.active(now)) { clear(target,type,ClearReason.EXPIRE); continue; }
                    if((type==StatusEffectType.POISON||type==StatusEffectType.SCARLET_ROT)&&now>=e.nextDot) {
                        e.nextDot=now+d.tickInterval(); damage(target,type,e,d,StatusResistanceService.resolve(target,type));
                    }
                } else {
                    e.decay(now,d);
                    if(e.current==0) clear(target,type,ClearReason.EXPIRE);
                }
            }
        }
    }
    private StatusRuntimeService() {}
}
