package dev.maplesadventure.progression.status;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import dev.maplesadventure.progression.*;

class StatusResistanceCurveTest {
    @Test void sevenStatusesShareFourCanonicalGroups() {
        assertEquals(7,StatusEffectType.values().length);
        assertEquals(StatusResistanceType.IMMUNITY,StatusEffectType.POISON.resistanceType());
        assertEquals(StatusEffectType.POISON.resistanceType(),StatusEffectType.SCARLET_ROT.resistanceType());
        assertEquals(StatusResistanceType.ROBUSTNESS,StatusEffectType.BLEED.resistanceType());
        assertEquals(StatusEffectType.BLEED.resistanceType(),StatusEffectType.FROSTBITE.resistanceType());
        assertEquals(StatusResistanceType.FOCUS,StatusEffectType.SLEEP.resistanceType());
        assertEquals(StatusEffectType.SLEEP.resistanceType(),StatusEffectType.MADNESS.resistanceType());
        assertEquals(StatusResistanceType.VITALITY,StatusEffectType.DEATH_BLIGHT.resistanceType());
    }
    @Test void initialResistanceIs160() {
        var snapshot=PlayerStatusResistanceCalculator.calculate(new PlayerAttributeState());
        for(var type:StatusResistanceType.values()) assertEquals(160,snapshot.value(type));
    }
    @Test void levelSoftcapsAreBounded() {
        assertEquals(0,StatusLevelResistanceCurve.evaluate(5)); assertEquals(16.5,StatusLevelResistanceCurve.evaluate(71));
        assertEquals(22.5,StatusLevelResistanceCurve.evaluate(111)); assertEquals(27.5,StatusLevelResistanceCurve.evaluate(161));
        assertEquals(27.5,StatusLevelResistanceCurve.evaluate(757));
    }
    @Test void attributeKnotsAndGovernance() {
        assertEquals(0,StandardResistanceAttributeCurve.evaluate(30)); assertEquals(30,StandardResistanceAttributeCurve.evaluate(40));
        assertEquals(40,StandardResistanceAttributeCurve.evaluate(60)); assertEquals(49.75,StandardResistanceAttributeCurve.evaluate(99));
        assertEquals(10,StandardResistanceAttributeCurve.vitality(15)); assertEquals(25,StandardResistanceAttributeCurve.vitality(40));
        assertEquals(35,StandardResistanceAttributeCurve.vitality(60)); assertEquals(44.75,StandardResistanceAttributeCurve.vitality(99));
        for(var type:StatusResistanceType.values()) {
            var s=PlayerStatusResistanceCalculator.calculate(new PlayerAttributeState().with(type.attribute(),40,99));
            assertTrue(s.values().get(type).attribute()>0);
            for(var other:StatusResistanceType.values()) if(other!=type) assertEquals(0,s.values().get(other).attribute());
        }
    }
}
