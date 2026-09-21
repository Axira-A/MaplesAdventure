package dev.maplesadventure.integration.api;

import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.resources.ResourceLocation;
import dev.maplesadventure.api.status.*;
import dev.maplesadventure.api.status.StatusApplyResult.Outcome;
import dev.maplesadventure.progression.ProgressionAttachments;
import dev.maplesadventure.progression.status.*;
import dev.maplesadventure.multiplayer.phase.PhaseRelations;

/** Internal conversion boundary. Public signatures never expose progression types. */
public final class StatusApiBridge {
    public static Outcome invalid(LivingEntity target) {
        if(target==null) return Outcome.INVALID_TARGET;
        if(!(target.level() instanceof ServerLevel level)) return Outcome.NOT_SERVER;
        if(!level.getServer().isSameThread()) return Outcome.WRONG_THREAD;
        if(target.isRemoved()||!target.isAlive()) return Outcome.INVALID_TARGET;
        return null;
    }
    public static StatusView snapshot(LivingEntity target,MaplesStatusType type) {
        var internal=StatusEffectType.valueOf(type.name());
        var resistance=StatusResistanceService.resolve(target,internal);
        var state=target.getExistingData(ProgressionAttachments.STATUS_RUNTIME).orElse(null);
        var entry=state==null?null:state.get(internal);
        long now=StatusRuntimeService.now(target);
        boolean locked=state!=null&&state.locked(now);
        return new StatusView(type,entry==null?0:entry.current,resistance.threshold(),resistance.immune(),
                entry!=null&&entry.active(now),resistance.correction(),StatusResistanceCorrectionService.offset(target,internal,resistance),
                state==null?0:state.procCount(internal),resistance.procDamageMultiplier(),
                locked?Optional.of(MaplesStatusType.valueOf(state.lockType.name())):Optional.empty(),
                locked?state.lockEnd-now:0);
    }
    public static Optional<StatusView> query(LivingEntity target,MaplesStatusType type) {
        return type==null||invalid(target)!=null?Optional.empty():Optional.of(snapshot(target,type));
    }
    private static StatusApplyResult failure(Outcome outcome) { return new StatusApplyResult(outcome,Optional.empty(),Optional.empty()); }
    private static StatusApplyResult result(Outcome outcome,StatusView before,LivingEntity target,MaplesStatusType type) {
        return new StatusApplyResult(outcome,Optional.of(before),Optional.of(snapshot(target,type)));
    }
    public static StatusApplyResult apply(LivingEntity target,MaplesStatusType type,double amount,StatusSource source,boolean proc) {
        var invalid=invalid(target); if(invalid!=null) return failure(invalid);
        if(type==null) return failure(Outcome.UNSUPPORTED_STATUS);
        if(ApiNotifications.busy()) return failure(Outcome.REENTRANT);
        if(source==null) return failure(Outcome.INVALID_SOURCE);
        if(!proc&&(!Double.isFinite(amount)||amount<=0||amount>100000)) return failure(Outcome.INVALID_AMOUNT);
        Entity actor=source.entity().orElse(null);
        if(actor!=null) {
            if(actor.level()!=target.level()||actor.isRemoved()) return failure(Outcome.INVALID_SOURCE);
            if(!PhaseRelations.canDamage(actor,target)) return failure(Outcome.PHASE_DENIED);
            if(actor instanceof Projectile p&&p.getOwner()!=null
                    &&(p.getOwner().level()!=target.level()||!PhaseRelations.canDamage(p.getOwner(),target))) return failure(Outcome.PHASE_DENIED);
        }
        var before=snapshot(target,type);
        if(before.immune()) return result(Outcome.IMMUNE,before,target,type);
        if(before.active()) return result(Outcome.ALREADY_ACTIVE,before,target,type);
        Entity owner=actor;
        for(int depth=0;depth<8&&owner instanceof Projectile p&&p.getOwner()!=null&&p.getOwner()!=owner;depth++) owner=p.getOwner();
        var internal=StatusEffectType.valueOf(type.name());
        var context=new StatusSourceContext(owner==null?null:owner.getUUID(),
                actor==null?StatusSourceContext.SourceKind.ENVIRONMENT:StatusSourceContext.SourceKind.ADAPTER,
                ResourceLocation.withDefaultNamespace("air"),actor instanceof Projectile,internal);
        boolean procced=StatusBuildupService.apply(target,internal,proc?before.threshold():amount,context);
        return result(procced?Outcome.PROCCED:Outcome.APPLIED,before,target,type);
    }
    public static StatusApplyResult clear(LivingEntity target,MaplesStatusType type,boolean buildupOnly) {
        var invalid=invalid(target); if(invalid!=null) return failure(invalid);
        if(type==null) return failure(Outcome.UNSUPPORTED_STATUS);
        if(ApiNotifications.busy()) return failure(Outcome.REENTRANT);
        var before=snapshot(target,type); var internal=StatusEffectType.valueOf(type.name());
        if(buildupOnly) StatusRuntimeService.clearBuildup(target,internal);
        else StatusRuntimeService.clear(target,internal,StatusRuntimeService.ClearReason.CURE);
        return result(Outcome.CLEARED,before,target,type);
    }
    private StatusApiBridge() {}
}
