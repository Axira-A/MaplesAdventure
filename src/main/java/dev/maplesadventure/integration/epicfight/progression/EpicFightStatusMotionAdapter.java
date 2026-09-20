package dev.maplesadventure.integration.epicfight.progression;

import dev.maplesadventure.progression.status.StatusMotionValueResolver;
import yesman.epicfight.api.animation.property.AnimationProperty;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;

/** Public, opt-in property for addon authors. DAMAGE_MODIFIER is not a status motion value. */
public final class EpicFightStatusMotionAdapter {
    public static final AnimationProperty.AttackPhaseProperty<Double> STATUS_MOTION_VALUE=new AnimationProperty.AttackPhaseProperty<>();
    public static void capture(AttackAnimation animation,AttackAnimation.Phase phase,EpicFightDamageSource source) {
        if(source==null||source.getAnimation()==null||phase==null) return;
        String name=source.getAnimation().registryName().toString(); int index=-1;
        for(int i=0;i<animation.phases.length;i++) if(animation.phases[i]==phase) { index=i; break; }
        Double explicit=phase.getProperty(STATUS_MOTION_VALUE).orElse(null);
        StatusMotionValueResolver.attach(source,new StatusMotionValueResolver.Attack(index<0?null:name+"#"+index,name,null,explicit));
    }
    private EpicFightStatusMotionAdapter() {}
}
