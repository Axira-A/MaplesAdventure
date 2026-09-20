package dev.maplesadventure.progression.status;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class StatusArcaneScalingCurveTest {
    @Test void knotsAreSeparateFromOffensiveCurve() {
        int[] stats={5,25,45,60,99}; double[] amounts={0,.10,.75,.90,1};
        for(int i=0;i<stats.length;i++) assertEquals(amounts[i],StatusArcaneScalingCurve.evaluate(stats[i]),1e-12);
        assertNotEquals(dev.maplesadventure.progression.OffensiveScalingCurve.evaluate(25),StatusArcaneScalingCurve.evaluate(25));
    }
    @Test void forbiddenSourcesCannotAcquireArcaneScaling() {
        for(var type:new StatusEffectType[]{StatusEffectType.FROSTBITE,StatusEffectType.SCARLET_ROT,StatusEffectType.DEATH_BLIGHT}) {
            assertThrows(IllegalArgumentException.class,()->new StatusBuildupComponent(type,30,.1));
            assertThrows(IllegalArgumentException.class,()->new StatusBuildupComponent(type,30,0,StatusArcaneScalingPolicy.FOLLOW_WEAPON_ARCANE));
            for(int arc:new int[]{5,25,45,60,99}) assertEquals(30,new StatusBuildupComponent(type,30,0).amount(arc));
        }
    }
    @Test void scalingIsSourceSpecificForAllFourPermittedTypes() {
        for(var type:new StatusEffectType[]{StatusEffectType.BLEED,StatusEffectType.POISON,StatusEffectType.SLEEP,StatusEffectType.MADNESS}) {
            assertEquals(28,new StatusBuildupComponent(type,28,0).amount(99));
            assertEquals(36.4,new StatusBuildupComponent(type,28,.30).amount(99),1e-12);
            assertEquals(42,new StatusBuildupComponent(type,28,0,StatusArcaneScalingPolicy.FOLLOW_WEAPON_ARCANE).amount(99,.5,1),1e-12);
        }
    }
    @Test void bloodCalibrationIsSixOrFiveHits() {
        var c=new StatusBuildupComponent(StatusEffectType.BLEED,28,.3);
        assertEquals(6,Math.ceil(160/c.amount(5))); assertEquals(5,Math.ceil(160/c.amount(99)));
    }
}
