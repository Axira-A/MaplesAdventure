package dev.maplesadventure.progression.status;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;

public final class StatusBuildupService {
    public static boolean apply(LivingEntity target,StatusEffectType type,double amount,StatusSourceContext source) {
        StatusResistance.bounded(amount,0,100000);
        if(target.level().isClientSide()||!target.isAlive()||target.isRemoved()||source==null||source.statusType()!=type) return false;
        // Public adapters cannot turn an incompatible live attacker into legal buildup.
        if(source.attackerUUID()!=null) {
            var actor=((ServerLevel)target.level()).getEntity(source.attackerUUID());
            if(actor!=null&&!dev.maplesadventure.multiplayer.phase.PhaseRelations.canDamage(actor,target)) return false;
        }
        var resistance=StatusResistanceService.resolve(target,type);
        if(resistance.immune()||StatusRuntimeService.active(target,type)||amount==0) return false;
        var state=StatusRuntimeService.state(target);
        boolean proc=state.accumulate(type,amount,resistance,source,StatusRuntimeService.now(target));
        if(proc) StatusRuntimeService.proc(target,type,resistance); else StatusRuntimeService.changed(target);
        return proc;
    }
    public static boolean proc(LivingEntity target,StatusEffectType type,StatusSourceContext source) {
        return apply(target,type,StatusResistanceService.resolve(target,type).threshold(),source);
    }
    private StatusBuildupService() {}
}
