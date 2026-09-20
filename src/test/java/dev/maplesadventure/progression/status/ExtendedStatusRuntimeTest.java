package dev.maplesadventure.progression.status;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ExtendedStatusRuntimeTest {
    @Test void percentDamageAndDurations() {
        assertEquals(15,StatusEffectDefinition.defaults(StatusEffectType.BLEED).damage(100,1));
        assertEquals(10.5,StatusEffectDefinition.defaults(StatusEffectType.BLEED).damage(100,.7),1e-12);
        var frost=StatusEffectDefinition.defaults(StatusEffectType.FROSTBITE);
        assertEquals(10,frost.damage(100,1)); assertEquals(7,frost.damage(100,.7),1e-12); assertEquals(600,frost.duration());
        assertEquals(1.2,frost.damageTakenMultiplier()); assertEquals(.8,frost.staminaRegenMultiplier());
        assertEquals(15,StatusEffectDefinition.defaults(StatusEffectType.MADNESS).damage(100,1));
    }
    @Test void dotsAreDistinctAndLethalScaleNotVanillaPoison() {
        var poison=StatusEffectDefinition.defaults(StatusEffectType.POISON); var rot=StatusEffectDefinition.defaults(StatusEffectType.SCARLET_ROT);
        assertEquals(1800,poison.duration()); assertEquals(20,poison.tickInterval()); assertEquals(1800,rot.duration());
        assertEquals(.09,poison.damage(20,1),1e-12); assertEquals(.18,rot.damage(20,1),1e-12);
        assertEquals(0,poison.flatDamage()); assertEquals(0,rot.flatDamage());
    }
    @Test void onlyThreeLocalDurationBars() {
        assertEquals(3,java.util.Arrays.stream(StatusEffectType.values()).filter(StatusEffectType::durationBar).count());
        assertEquals(0,StatusEffectDefinition.defaults(StatusEffectType.SLEEP).duration());
        assertEquals(0,StatusEffectDefinition.defaults(StatusEffectType.MADNESS).duration());
    }
    @Test void fullBlockDoesNotBuildStatus() {
        assertFalse(StatusGuardPolicy.permitsBuildup(0)); assertFalse(StatusGuardPolicy.permitsBuildup(Double.NaN));
        assertTrue(StatusGuardPolicy.permitsBuildup(.01));
    }
}
