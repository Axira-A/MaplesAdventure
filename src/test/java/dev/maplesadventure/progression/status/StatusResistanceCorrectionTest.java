package dev.maplesadventure.progression.status;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class StatusResistanceCorrectionTest {
    @Test void stagesReplaceInsteadOfAccumulate() {
        double[] totals={220,241,269,311,409,699,699};
        for(int i=0;i<totals.length;i++) assertEquals(totals[i],220+StatusResistanceCorrectionProfile.STANDARD.offset(i));
        assertEquals(0,StatusResistanceCorrectionProfile.NONE.offset(999));
    }
    @Test void correctionOutlivesEmptyBarsAndIsPerStatus() {
        var s=new StatusRuntimeState();
        for(int i=0;i<6;i++) s.accumulate(StatusEffectType.BLEED,10000,StatusResistance.DEFAULT,StatusSourceContext.admin(StatusEffectType.BLEED),i);
        s.remove(StatusEffectType.BLEED);
        assertEquals(5,s.procCount(StatusEffectType.BLEED)); assertEquals(0,s.procCount(StatusEffectType.FROSTBITE));
        var copy=new StatusRuntimeState(); copy.deserializeNBT(null,s.serializeNBT(null)); assertEquals(5,copy.procCount(StatusEffectType.BLEED));
        copy.accumulate(StatusEffectType.FROSTBITE,10000,StatusResistance.DEFAULT,StatusSourceContext.admin(StatusEffectType.FROSTBITE),10);
        copy.resetCorrection(StatusEffectType.BLEED); assertEquals(0,copy.procCount(StatusEffectType.BLEED)); assertEquals(1,copy.procCount(StatusEffectType.FROSTBITE));
        copy.resetCorrections(); assertEquals(0,copy.procCount(StatusEffectType.BLEED));
    }
}
