package dev.maplesadventure.progression.status;

import dev.maplesadventure.progression.ProgressionAttachments;
import net.minecraft.world.entity.*;
import net.minecraft.server.level.ServerPlayer;

/** Independent server authority, not an animation or permanently changed noAI flag. */
public final class StatusControlLockService {
    public static boolean locked(LivingEntity entity) {
        if(entity.level().isClientSide()) return entity instanceof net.minecraft.world.entity.player.Player player&&player.isLocalPlayer() && dev.maplesadventure.progression.status.client.ClientStatusState.controlLocked();
        var state=entity.getExistingData(ProgressionAttachments.STATUS_RUNTIME).orElse(null);
        return state!=null&&state.locked(StatusRuntimeService.now(entity));
    }
    public static void lock(LivingEntity entity,StatusEffectType type,int ticks,boolean deep) {
        if(entity.level().isClientSide()) return;
        if(type!=StatusEffectType.SLEEP&&type!=StatusEffectType.MADNESS||ticks<1||ticks>1200) throw new IllegalArgumentException("Control lock bounds");
        var state=StatusRuntimeService.state(entity); long now=StatusRuntimeService.now(entity);
        state.lockType=type; state.lockStart=now; state.lockEnd=now+ticks; state.deepSleep=deep;
        entity.stopUsingItem(); entity.setSprinting(false);
        if(entity instanceof Mob mob) mob.getNavigation().stop();
        state.changed(); StatusRuntimeService.changed(entity);
    }
    public static void wakeOnHit(LivingEntity target) {
        var state=target.getExistingData(ProgressionAttachments.STATUS_RUNTIME).orElse(null);
        if(state!=null&&state.deepSleep) { state.unlock(); StatusRuntimeService.changed(target); }
    }
    static void tick(LivingEntity entity,long now,StatusRuntimeState state) {
        if(state.lockType==null) return;
        if(!state.locked(now)) { state.unlock(); StatusRuntimeService.changed(entity); return; }
        entity.setSprinting(false); entity.stopUsingItem();
        if(entity instanceof Mob mob) { mob.getNavigation().stop(); mob.setTarget(null); }
        var velocity=entity.getDeltaMovement(); entity.setDeltaMovement(0,Math.min(0,velocity.y),0);
    }
    private StatusControlLockService() {}
}
