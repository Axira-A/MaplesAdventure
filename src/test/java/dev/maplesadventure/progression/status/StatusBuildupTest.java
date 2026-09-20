package dev.maplesadventure.progression.status;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import dev.maplesadventure.progression.weapon.*;

class StatusBuildupTest {
    @Test void thresholdAndOverflowTriggerOnlyOnce() {
        var s=new StatusRuntimeState(); var t=StatusEffectType.BLEED;
        assertFalse(s.accumulate(t,90,StatusResistance.DEFAULT,StatusSourceContext.admin(t),0));
        assertTrue(s.accumulate(t,250,StatusResistance.DEFAULT,StatusSourceContext.admin(t),1));
        assertEquals(0,s.get(t).current); assertEquals(1,s.get(t).procSerial);
        assertFalse(s.accumulate(t,30,StatusResistance.DEFAULT,StatusSourceContext.admin(t),2));
        assertEquals(30,s.get(t).current);
    }
    @Test void resistanceChangesHitsNotDamageDefense() {
        for(var pair:List.of(new int[]{100,4},new int[]{200,7})) {
            var s=new StatusRuntimeState(); int procs=0;
            for(int hit=0;hit<pair[1];hit++) if(s.accumulate(StatusEffectType.BLEED,30,new StatusResistance(pair[0],false,1),StatusSourceContext.admin(StatusEffectType.BLEED),hit)) procs++;
            assertEquals(1,procs);
        }
    }
    @Test void immuneNeverCreatesEntry() {
        var s=new StatusRuntimeState();
        assertFalse(s.accumulate(StatusEffectType.BLEED,100000,new StatusResistance(100,true,1),StatusSourceContext.admin(StatusEffectType.BLEED),1)); assertTrue(s.empty());
    }
    @Test void arcUsesExistingCurveOnlyForAmount() {
        var c=new StatusBuildupComponent(StatusEffectType.BLEED,30,.55);
        assertEquals(30,c.amount(5)); assertEquals(46.5,c.amount(99),1e-10);
        for(var type:StatusEffectType.values()) {
            var d=StatusEffectDefinition.defaults(type);
            assertEquals(d.damage(100,1),StatusEffectDefinition.defaults(type).damage(100,1));
            assertEquals(d.duration(),StatusEffectDefinition.defaults(type).duration());
        }
        assertTrue(c.amount(40)>c.amount(5));
    }
    @Test void canonicalStatusInfusionsAreExplicit() {
        for(var definition:WeaponInfusionRegistry.definitions().values()) {
            boolean expected=Set.of("blood","poison","cold","slumber","frenzied","rot","blight").contains(definition.id().getPath());
            assertEquals(expected,!definition.statuses().components().isEmpty());
        }
    }
    @Test void duplicateContributionsHaveFiniteCap() {
        var c=new StatusBuildupComponent(StatusEffectType.BLEED,1000,2);
        var p=new WeaponStatusProfile(List.of(c,c));
        assertEquals(3000,p.evaluate(99).amounts().get(StatusEffectType.BLEED));
        assertThrows(IllegalArgumentException.class,()->new StatusBuildupComponent(StatusEffectType.POISON,Double.NaN,1));
    }
}
