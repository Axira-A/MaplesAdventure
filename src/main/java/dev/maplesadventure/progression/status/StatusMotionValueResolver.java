package dev.maplesadventure.progression.status;

import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.core.registries.BuiltInRegistries;

/** Exact source metadata, not event timing or attack speed. Reload compiles all lookups. */
public final class StatusMotionValueResolver {
    public record Attack(String exact, String animation, String skill, Double explicit) {
        public Attack { for(String key:new String[]{exact,animation,skill}) if(key!=null&&key.length()>256) throw new IllegalArgumentException("Attack ID length"); if(explicit!=null) StatusResistance.bounded(explicit,0,4); }
    }
    public record Decision(double value,String source) { public Decision { StatusResistance.bounded(value,0,4); } }
    private static final Map<DamageSource,Attack> ATTACKS=Collections.synchronizedMap(new WeakHashMap<>());
    public static void attach(DamageSource source,Attack attack) { ATTACKS.put(source,attack); }
    public static Decision resolve(DamageSource source,String archetype) { return resolve(ATTACKS.get(source),archetype,null); }
    public static Decision projectile(Projectile projectile,String archetype) {
        return resolve(null,archetype,BuiltInRegistries.ENTITY_TYPE.getKey(projectile.getType()).toString());
    }
    public static Decision resolve(Attack attack,String archetype,String projectile) {
        if(attack!=null&&attack.explicit()!=null) return new Decision(attack.explicit(),"INTEGRATION");
        var tables=StatusMotionRules.tables();
        if(attack!=null) {
            var exact=attack.exact()==null?null:tables.get("attack").get(attack.exact()); if(exact!=null) return new Decision(exact,"EXACT_ATTACK");
            var skill=attack.skill()==null?null:tables.get("skill").get(attack.skill()); if(skill!=null) return new Decision(skill,"SKILL");
            var animation=attack.animation()==null?null:tables.get("animation").get(attack.animation()); if(animation!=null) return new Decision(animation,"ANIMATION");
        }
        var shot=projectile==null?null:tables.get("projectile").get(projectile); if(shot!=null) return new Decision(shot,"PROJECTILE");
        var weapon=archetype==null?null:tables.get("archetype").get(archetype); if(weapon!=null) return new Decision(weapon,"ARCHETYPE");
        return new Decision(1,"FALLBACK");
    }
    public static StatusBuildupSnapshot apply(StatusBuildupSnapshot snapshot,double motion) {
        StatusResistance.bounded(motion,0,4);
        var values=new EnumMap<StatusEffectType,Double>(StatusEffectType.class);
        snapshot.amounts().forEach((t,v)->values.put(t,Math.min(StatusBuildupSnapshot.MAX_PER_STATUS,v*motion)));
        return new StatusBuildupSnapshot(values);
    }
    public static void clear() { ATTACKS.clear(); StatusMotionRules.reset(); }
    private StatusMotionValueResolver() {}
}
