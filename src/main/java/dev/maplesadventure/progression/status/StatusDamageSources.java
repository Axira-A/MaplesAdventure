package dev.maplesadventure.progression.status;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;

/** Established ailments belong to the target; attribution must not revoke them when their source leaves a phase. */
public final class StatusDamageSources {
    public static boolean isQuietDot(DamageSource source) {
        return source.is(ResourceKey.create(Registries.DAMAGE_TYPE,StatusEffectType.POISON.definitionId()))
                ||source.is(ResourceKey.create(Registries.DAMAGE_TYPE,StatusEffectType.SCARLET_ROT.definitionId()));
    }
    public static boolean isStatus(DamageSource source) {
        for(var type:StatusEffectType.values()) if(source.is(ResourceKey.create(Registries.DAMAGE_TYPE,type.definitionId()))) return true;
        return false;
    }
    public static DamageSource create(LivingEntity target,StatusEffectType type,StatusSourceContext context) {
        var level=(ServerLevel)target.level(); Entity owner=null;
        if(context!=null&&context.attackerUUID()!=null) {
            owner=level.getEntity(context.attackerUUID());
            if(owner!=null&&!dev.maplesadventure.multiplayer.phase.PhaseRelations.canDamage(owner,target)) owner=null;
        }
        var holder=level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE,type.definitionId()));
        return owner==null?new DamageSource(holder):new DamageSource(holder,owner);
    }
    private StatusDamageSources() {}
}
