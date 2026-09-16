package dev.maplesadventure.progression;

import dev.maplesadventure.progression.defense.*;
import dev.maplesadventure.progression.stats.*;
import dev.maplesadventure.progression.weapon.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlayerDefenseCalculatorTest {
    static PlayerAttributeState all(int value) {
        var state=PlayerAttributeState.defaultsState();
        for(var attribute:Attribute.values()) state=state.with(attribute,value,99);
        return state;
    }
    @Test void starterAllTenNoArmorContribution() {
        var p=PlayerDefenseService.preview(all(5));
        for(var ch:WeaponDamageChannel.values()) {
            assertEquals(10,p.channel(ch).defense()); assertEquals(0,p.channel(ch).absorption());
            assertEquals(0,p.values().get(ch).equipment()); assertEquals(0,p.values().get(ch).effect());
        }
    }
    @Test void fortyExactExistingFormula() { verify(40,new double[]{27.5,22.25,25.75,22.25,24,22.25,22.25,22.25,22.25}); }
    @Test void ninetyNineExactExistingFormula() { verify(99,new double[]{57,42.9,52.3,42.9,47.6,42.9,42.9,42.9,42.9}); }
    private void verify(int stat,double[] expected) {
        var p=PlayerDefenseService.preview(all(stat));
        int i=0; for(var ch:WeaponDamageChannel.values()) assertEquals(expected[i++],p.channel(ch).defense(),1e-12,ch.name());
    }
    @Test void runtimeAndCharacterStatsAreIdenticalActive() {
        for(int stat:new int[]{5,20,40,60,99}) {
            var p=PlayerDefenseService.preview(all(stat)); var ui=CharacterStatCalculator.calculate(all(stat));
            for(var type:DamageDefenseType.values()) {
                assertEquals(p.channel(type.channel()).defense(),ui.value(type.stat()).value());
                assertEquals(StatImplementationState.ACTIVE,ui.value(type.stat()).implementation());
            }
        }
    }
    @Test void previewNeverMutatesBaselineAndOnlyChangesRelevantDefense() {
        var baseline=all(39); var next=baseline.with(Attribute.VIGOR,40,99);
        var before=PlayerDefenseService.preview(baseline); var after=PlayerDefenseService.preview(next);
        assertEquals(39,baseline.get(Attribute.VIGOR));
        assertEquals(.25,after.channel(WeaponDamageChannel.PHYSICAL).defense()-before.channel(WeaponDamageChannel.PHYSICAL).defense(),1e-12);
        assertEquals(before.channel(WeaponDamageChannel.MAGIC),after.channel(WeaponDamageChannel.MAGIC));
    }
    @Test void statusThresholdComesFromExplicitSnapshotNotAttributeFormula() {
        var state=all(99);
        var snapshot=CharacterStatCalculator.calculate(state,
                dev.maplesadventure.progression.runtime.RuntimeResourceSnapshot.progressionOnly(state),
                dev.maplesadventure.progression.encumbrance.EquipLoadRuntimeSnapshot.unavailable(),
                dev.maplesadventure.progression.spell.SpellSchoolScalingSnapshot.empty(),
                java.util.Map.of(dev.maplesadventure.progression.status.StatusEffectType.BLEED,173.0));
        assertEquals(173,snapshot.value(CharacterStat.BLEED_RESISTANCE).value());
        assertEquals(StatImplementationState.ACTIVE,snapshot.value(CharacterStat.BLEED_RESISTANCE).implementation());
    }
}
