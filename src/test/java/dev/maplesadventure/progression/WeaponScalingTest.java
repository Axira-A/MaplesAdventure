package dev.maplesadventure.progression;

import com.google.gson.JsonParser;
import dev.maplesadventure.progression.weapon.*;
import dev.maplesadventure.progression.stats.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class WeaponScalingTest {
    private PlayerAttributeState stats(int value) {
        var s=PlayerAttributeState.defaultsState();
        for(var a:WeaponRequirementProfile.ATTRIBUTES) s=s.with(a,value,99);
        return s;
    }
    @Test void gradeBoundariesArePresentationOnly() {
        double[] values={0,.0999,.10,.2499,.25,.3999,.40,.5499,.55,.6999,.70,.8499,.85,1.5};
        ScalingGrade[] grades={ScalingGrade.NONE,ScalingGrade.NONE,ScalingGrade.E,ScalingGrade.E,ScalingGrade.D,ScalingGrade.D,
                ScalingGrade.C,ScalingGrade.C,ScalingGrade.B,ScalingGrade.B,ScalingGrade.A,ScalingGrade.A,ScalingGrade.S,ScalingGrade.S};
        for(int i=0;i<values.length;i++) assertEquals(grades[i],ScalingGrade.of(values[i]));
        var p=WeaponScalingProfile.automatic(WeaponRequirementArchetype.TOOL);
        assertEquals(ScalingGrade.NONE,ScalingGrade.of(p.dexterity()));
        assertEquals(.05,WeaponAttackRatingCalculator.contribution(stats(99),p,Attribute.DEXTERITY),1e-9);
    }
    @Test void swordReusesExactExistingCurveAndInitialZero() {
        var p=WeaponScalingProfile.automatic(WeaponRequirementArchetype.SWORD);
        for(int value:new int[]{5,20,40,60,99}) {
            var r=WeaponAttackRatingCalculator.calculate(7,p,stats(value));
            assertEquals(7*(1+.70*OffensiveScalingCurve.evaluate(value)),r.attackRating(),1e-9);
        }
        assertEquals(0,WeaponAttackRatingCalculator.calculate(4,p,stats(5)).bonusAttack());
    }
    @Test void archetypeTiltsDoNotDependOnMaterialOrMagicAffinity() {
        for(var type:WeaponRequirementArchetype.values()) {
            var p=WeaponScalingProfile.automatic(type);
            assertEquals(0,p.intelligence()); assertEquals(0,p.faith()); assertEquals(0,p.arcane());
        }
        var str=stats(5).with(Attribute.STRENGTH,40,99); var dex=stats(5).with(Attribute.DEXTERITY,40,99);
        var great=WeaponScalingProfile.automatic(WeaponRequirementArchetype.GREATSWORD);
        var dagger=WeaponScalingProfile.automatic(WeaponRequirementArchetype.DAGGER);
        assertTrue(WeaponAttackRatingCalculator.effectiveScaling(str,great)>WeaponAttackRatingCalculator.effectiveScaling(dex,great)*4);
        assertTrue(WeaponAttackRatingCalculator.effectiveScaling(dex,dagger)>WeaponAttackRatingCalculator.effectiveScaling(str,dagger)*6);
    }
    @Test void capsAndFiniteBounds() {
        var p=new WeaponScalingProfile(1.5,1.5,1.5,1.5,1.5,2,"TEST","TEST","");
        var r=WeaponAttackRatingCalculator.calculate(10000,p,stats(99));
        assertEquals(7.5,r.scalingFactor()); assertEquals(3,r.damageMultiplier()); assertEquals(30000,r.attackRating());
        for(double invalid:new double[]{-.1,1.51,Double.NaN,Double.POSITIVE_INFINITY})
            assertThrows(IllegalArgumentException.class,()->new WeaponScalingProfile(invalid,0,0,0,0,1.15,"TEST","TEST",""));
        assertThrows(IllegalArgumentException.class,()->new WeaponScalingProfile(0,0,0,0,0,2.01,"TEST","TEST",""));
        assertThrows(IllegalArgumentException.class,()->WeaponAttackRatingCalculator.calculate(Double.NaN,p,stats(5)));
    }
    @Test void requirementPenaltyNeverChangesNominalAr() {
        var s=stats(5).with(Attribute.STRENGTH,99,99);
        var req=new WeaponRequirementProfile(24,11,0,0,0,"TEST","GREATSWORD","");
        var result=WeaponRequirementService.evaluate(s,req,.35);
        var p=WeaponScalingProfile.automatic(WeaponRequirementArchetype.GREATSWORD);
        var r=WeaponAttackRatingCalculator.calculate(10,p,s);
        var snapshot=WeaponAttackSnapshot.of(ResourceLocation.parse("test:weapon"),r,p,result);
        assertEquals(17.5,snapshot.attackRating()); assertEquals(.35,snapshot.effectiveRequirementMultiplier());
        assertEquals(.6125,r.damageMultiplier()*result.damageMultiplier(),1e-9);
    }
    @Test void draftRecalculatesAllStatsWithoutChangingBaseline() {
        var s=stats(20); var p=WeaponScalingProfile.automatic(WeaponRequirementArchetype.LONGSWORD);
        var held=new WeaponLoadoutSnapshot.Held(ResourceLocation.parse("test:sword"),WeaponRequirementProfile.NONE,p,10,true);
        var loadout=new WeaponLoadoutSnapshot(held,WeaponLoadoutSnapshot.empty().offHand(),.35);
        var draft=LevelUpPreviewCalculator.calculate(s,Map.of(Attribute.STRENGTH,2,Attribute.DEXTERITY,3),100000,99,1);
        var view=loadout.evaluate(draft.state()).getFirst();
        assertEquals(10*(1+.5*OffensiveScalingCurve.evaluate(22)+.3*OffensiveScalingCurve.evaluate(23)),view.attack().attackRating(),1e-9);
        assertEquals(20,s.get(Attribute.STRENGTH));
        var charStats=new CharacterStatsSnapshot(5,Map.of()).withWeapons(List.of(view));
        assertEquals(StatImplementationState.ACTIVE,charStats.value(CharacterStat.MAIN_HAND_ATTACK).implementation());
        assertFalse(charStats.value(CharacterStat.OFF_HAND_ATTACK).available());
    }
    @Test void projectileSerializationAndLegacyFallback() {
        var old=new CompoundTag(); old.putDouble("multiplier",.35); old.putBoolean("qualified",false);
        var legacy=new ProjectileRequirementPenalty(); legacy.deserializeNBT(null,old);
        assertEquals(1,legacy.scalingMultiplier); assertEquals(.35,legacy.combatMultiplier());
        var launch=new ProjectileRequirementPenalty(UUID.randomUUID(),WeaponRequirementService.evaluate(stats(5),new WeaponRequirementProfile(20,20,0,0,0,"TEST","TEST",""),.35),1.8);
        var restored=new ProjectileRequirementPenalty(); restored.deserializeNBT(null,launch.serializeNBT(null));
        assertEquals(launch.owner,restored.owner); assertEquals(.63,restored.combatMultiplier(),1e-9);
        assertEquals(1.8,restored.scalingMultiplier); assertFalse(restored.qualified);
        var corrupt=launch.serializeNBT(null); corrupt.putDouble("scalingMultiplier",Double.NaN); restored.deserializeNBT(null,corrupt);
        assertEquals(1,restored.scalingMultiplier);
    }
    @Test void jsonOverridesAreIndependentAndStrict() {
        var id=ResourceLocation.parse("test:rule");
        var exact=WeaponScalingRules.parse(id,JsonParser.parseString("{\"item\":\"test:moon\",\"scaling\":{\"strength\":0.1,\"intelligence\":0.8}}").getAsJsonObject());
        assertEquals("EXACT_JSON",exact.profile().source()); assertEquals(.8,exact.profile().intelligence());
        var disabled=WeaponScalingRules.parse(id,JsonParser.parseString("{\"tag\":\"test:weapons\",\"disabled\":true}").getAsJsonObject());
        assertFalse(disabled.profile().enabled());
        for(String value:new String[]{"-1","1.51","1e309","\"0.7\""})
            assertThrows(RuntimeException.class,()->WeaponScalingRules.parse(id,JsonParser.parseString("{\"item\":\"test:weapon\",\"scaling\":{\"strength\":"+value+"}}").getAsJsonObject()));
        assertThrows(RuntimeException.class,()->WeaponScalingRules.parse(id,JsonParser.parseString("{\"item\":\"test:weapon\",\"scaling\":{\"vigor\":0.5}}").getAsJsonObject()));
    }
}
