package dev.maplesadventure.progression;

import com.google.gson.JsonParser;
import dev.maplesadventure.progression.spell.*;
import dev.maplesadventure.progression.stats.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SpellSchoolScalingTest {
    private static SpellSchoolScalingProfile profile(String name) {
        var path = "/data/maplesadventure/spell_school_scaling/" + name + ".json";
        try (var in = SpellSchoolScalingTest.class.getResourceAsStream(path)) {
            assertNotNull(in, path);
            return SpellSchoolScalingProfile.parse(JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject());
        } catch (java.io.IOException failure) { throw new AssertionError(failure); }
    }
    private static PlayerAttributeState stats(int i, int f, int a) {
        return PlayerAttributeState.defaultsState().with(Attribute.INTELLIGENCE, i, 99)
                .with(Attribute.FAITH, f, 99).with(Attribute.ARCANE, a, 99);
    }
    @Test void sharedCurveAnchorsAndSoftCaps() {
        int[] points = {5,20,40,60,99}; double[] expected = {0, .3015075377, .6030150754, .8040201005, 1};
        for (int i = 0; i < points.length; i++) assertEquals(expected[i], OffensiveScalingCurve.evaluate(points[i]), 1e-9);
        assertTrue(OffensiveScalingCurve.evaluate(20)-OffensiveScalingCurve.evaluate(19)
                > OffensiveScalingCurve.evaluate(40)-OffensiveScalingCurve.evaluate(39));
    }
    @Test void allNineDefaultProfilesBaselineAndCap() {
        for (String s : List.of("nature","lightning","ice","ender","eldritch","blood","evocation","holy","fire")) {
            var p = profile(s);
            assertEquals(ResourceLocation.parse("irons_spellbooks:" + s), p.schoolId());
            assertEquals(0, p.bonus(stats(5,5,5)), 1e-9);
            assertEquals(.75, p.bonus(stats(99,99,99)), 1e-9);
        }
    }
    @Test void mainAndSecondaryBuildsMatchWeights() {
        assertEquals(.6, profile("ender").bonus(stats(99,5,5)), 1e-9);
        assertEquals(.75, profile("ender").bonus(stats(99,5,99)), 1e-9);
        assertEquals(.3844221105, profile("nature").bonus(stats(40,5,5)), 1e-9);
        assertEquals(.4070351759, profile("ice").bonus(stats(40,5,5)), 1e-9);
        assertEquals(.4522613065, profile("holy").bonus(stats(5,40,5)), 1e-9);
        assertEquals(.3844221105, profile("blood").bonus(stats(5,5,40)), 1e-9);
    }
    @Test void previewRetainsOtherModifiersAndGlobalPowerAndClamp() {
        // 1 base + 20% equipment ADD_VALUE; 10% ADD_MULTIPLIED_BASE; 20% ADD_MULTIPLIED_TOTAL.
        var context = new SpellPowerContext(1.2, 1.1*1.2, -100,100,1.3, 1.584,true);
        assertEquals((1.2+.3)*1.1*1.2*1.3, context.effectivePower(.3), 1e-9);
        assertEquals((1.2+.4)*1.1*1.2*1.3, context.effectivePower(.4), 1e-9);
        var clamped = new SpellPowerContext(99.9, 1, -100,100,2,100,true);
        assertEquals(200, clamped.effectivePower(.6), 1e-9);
        assertEquals(199.8, clamped.effectivePower(0), 1e-9);
    }
    @Test void dynamicAddonSchoolAndPureDraft() {
        var p = new SpellSchoolScalingProfile(ResourceLocation.parse("test:custom_school"), 1,0,0,.75);
        var before = stats(39,5,5);
        var ctx = new SpellPowerContext(1.2,1,-100,100,1,1.2+p.bonus(before),true);
        var s = new SpellSchoolStat(p, Component.literal("Test"), p.bonus(before), ctx,true);
        var schools = new SpellSchoolScalingSnapshot(Map.of(p.schoolId(),s));
        var ordinary = new AttributeSnapshot(before, AttributeProgression.level(before),20,100,20,100,
                dev.maplesadventure.progression.runtime.RuntimeResourceSnapshot.progressionOnly(before),
                dev.maplesadventure.progression.encumbrance.EquipLoadRuntimeSnapshot.unavailable());
        var baseline = new AttributeSnapshot(before, ordinary.level(),ordinary.maxHealth(),ordinary.mana(),ordinary.stamina(),
                ordinary.nextLevelCost(),ordinary.runtimeResources(),ordinary.equipLoad(),schools);
        var preview = LevelUpPreviewCalculator.calculate(baseline, Map.of(Attribute.INTELLIGENCE,1), 100000,99,1);
        assertEquals(39, before.get(Attribute.INTELLIGENCE));
        assertEquals(1.2+p.bonus(stats(40,5,5)), preview.characterStats().spellSchools().schools().get(p.schoolId()).runtimeSchoolPower(),1e-9);
        assertEquals(1, preview.characterStats().spellSchools().changedFrom(schools).size());
        assertEquals(ordinary.mana(),preview.mana());
        assertEquals(StatImplementationState.ACTIVE,preview.characterStats().value(CharacterStat.INTELLIGENCE_SCALING).implementation());
    }
    @Test void invalidDataIsRejectedIncludingUnknownAttribute() {
        for (String weights : List.of("{\"intelligence\":-1,\"faith\":2}", "{\"strength\":1}",
                "{\"intelligence\":0.8}", "{\"intelligence\":\"NaN\"}", "{\"intelligence\":1e999}"))
            assertThrows(RuntimeException.class, () -> SpellSchoolScalingProfile.parse(JsonParser.parseString(
                    "{\"school\":\"test:x\",\"weights\":"+weights+",\"max_bonus\":0.75}").getAsJsonObject()));
    }
    @Test void changedSchoolsSortedByActualDeltaNotFixedSchoolOrder() {
        var base = stats(5,5,5); var next = stats(5,40,5);
        Map<ResourceLocation, SpellSchoolStat> entries = new HashMap<>();
        for(String id : List.of("fire","holy","evocation","blood","ender")) {
            var p=profile(id); entries.put(p.schoolId(),new SpellSchoolStat(p,Component.literal(id),p.bonus(base),SpellPowerContext.unavailable(),false));
        }
        var old = new SpellSchoolScalingSnapshot(entries);
        assertEquals("holy",old.preview(next).changedFrom(old).getFirst().schoolId().getPath());
        assertEquals(4,old.preview(next).changedFrom(old).size());
        assertEquals(StatImplementationState.PREVIEW_ONLY,old.preview(next).schools().values().iterator().next().implementationState());
    }
}
