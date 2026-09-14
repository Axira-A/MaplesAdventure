package dev.maplesadventure.progression;

import com.google.gson.JsonParser;
import dev.maplesadventure.progression.weapon.*;
import dev.maplesadventure.progression.spell.SpellSchoolScalingProfile;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class WeaponRequirementTest {
    private WeaponRequirementProfile generate(WeaponRequirementArchetype type,double weight,double damage,double speed) {
        return WeaponRequirementHeuristic.generate(new WeaponFacts(type,"TEST",type.name(),weight,damage,speed,Map.of()),Map.of(),40);
    }
    @Test void starterWeaponsAreUsableAndGreatswordsAreNot() {
        var fresh=PlayerAttributeState.defaultsState();
        for(var type:new WeaponRequirementArchetype[]{WeaponRequirementArchetype.SWORD,WeaponRequirementArchetype.TOOL})
            assertTrue(WeaponRequirementService.evaluate(fresh,generate(type,0,4,1.6),.35).satisfied());
        assertFalse(WeaponRequirementService.evaluate(fresh,generate(WeaponRequirementArchetype.GREATSWORD,13,12,1),.35).satisfied());
    }
    @Test void boundedNonlinearWeightAndAutomaticCap() {
        var light=generate(WeaponRequirementArchetype.GREATSWORD,7,10,1);
        var heavy=generate(WeaponRequirementArchetype.GREATSWORD,13,10,1);
        assertTrue(heavy.strength()>light.strength());
        assertTrue(heavy.strength()-light.strength()<6);
        var extreme=generate(WeaponRequirementArchetype.GREATAXE,10000,100000,.01);
        assertTrue(extreme.strength()<=40); assertTrue(extreme.dexterity()<=40);
    }
    @Test void onePointShortAndPureDraftUnlock() {
        var baseline=PlayerAttributeState.defaultsState().with(Attribute.STRENGTH,19,99);
        var p=new WeaponRequirementProfile(20,5,0,0,0,"TEST","SWORD","");
        var before=WeaponRequirementService.evaluate(baseline,p,.35);
        assertEquals(Map.of(Attribute.STRENGTH,1),before.missingAttributes());
        assertEquals(.35,before.damageMultiplier()); assertFalse(before.weaponSkillAllowed());
        var after=WeaponRequirementService.evaluate(baseline.with(Attribute.STRENGTH,20,99),p,.35);
        assertTrue(after.satisfied()); assertTrue(after.weaponSkillAllowed()); assertEquals(1,after.damageMultiplier());
        assertEquals(19,baseline.get(Attribute.STRENGTH));
    }
    @Test void affinityUsesConfiguredSchoolWeightsNotNames() {
        var id=ResourceLocation.parse("test:unusual_school");
        var school=new SpellSchoolScalingProfile(id,.8,0,.2,.75);
        var f=new WeaponFacts(WeaponRequirementArchetype.SWORD,"TEST","SWORD",5,8,1.6,Map.of(id,.25));
        var p=WeaponRequirementHeuristic.generate(f,Map.of(id,school),40);
        assertEquals(12,p.intelligence()); assertEquals(0,p.faith()); assertEquals(5,p.arcane());
        assertEquals(0,WeaponRequirementHeuristic.generate(f,Map.of(),40).intelligence());
    }
    @Test void exactAndTagRulesValidateAndDisabledBypasses() {
        var id=ResourceLocation.parse("test:rule");
        var exact=WeaponRequirementRules.parse(id,JsonParser.parseString("{\"item\":\"minecraft:stick\",\"requirements\":{\"strength\":99}}").getAsJsonObject());
        assertEquals("EXACT_JSON",exact.profile().source()); assertEquals(99,exact.profile().strength());
        var disabled=WeaponRequirementRules.parse(id,JsonParser.parseString("{\"tag\":\"test:weapons\",\"disabled\":true}").getAsJsonObject());
        assertTrue(WeaponRequirementService.evaluate(PlayerAttributeState.defaultsState(),disabled.profile(),.35).satisfied());
        for(String req:new String[]{"{\"strength\":100}","{\"dexterity\":-1}","{\"strength\":5.5}","{\"vigor\":10}","{\"unknown\":5}"})
            assertThrows(RuntimeException.class,()->WeaponRequirementRules.parse(id,JsonParser.parseString("{\"item\":\"minecraft:stick\",\"requirements\":"+req+"}").getAsJsonObject()));
    }
    @Test void nonWeaponsAndInvalidPenalties() {
        var fresh=PlayerAttributeState.defaultsState();
        assertEquals(WeaponRequirementProfile.NONE,generate(WeaponRequirementArchetype.NONE,100,100,1));
        assertEquals(1,WeaponRequirementService.evaluate(fresh,WeaponRequirementProfile.NONE,.35).damageMultiplier());
        assertThrows(IllegalArgumentException.class,()->WeaponRequirementService.evaluate(fresh,WeaponRequirementProfile.NONE,Double.NaN));
    }
    @Test void rangedDoesNotMistakeUnarmedMeleeDefaultsForDrawSpeed() {
        var bow=generate(WeaponRequirementArchetype.BOW,0,1,4);
        var crossbow=generate(WeaponRequirementArchetype.CROSSBOW,0,1,4);
        assertEquals(5,bow.strength()); assertEquals(12,bow.dexterity());
        assertEquals(9,crossbow.strength()); assertEquals(10,crossbow.dexterity());
    }
    @Test void projectileSnapshotSurvivesSerializationAndLaterStats() {
        var owner=java.util.UUID.randomUUID();
        var p=generate(WeaponRequirementArchetype.GREATSWORD,13,12,1);
        var state=PlayerAttributeState.defaultsState();
        var launch=new ProjectileRequirementPenalty(owner,WeaponRequirementService.evaluate(state,p,.35));
        var restored=new ProjectileRequirementPenalty();
        restored.deserializeNBT(null,launch.serializeNBT(null));
        assertEquals(owner,restored.owner); assertFalse(restored.qualified); assertEquals(.35,restored.multiplier);
        state=state.with(Attribute.STRENGTH,99,99).with(Attribute.DEXTERITY,99,99);
        assertTrue(WeaponRequirementService.evaluate(state,p,.35).satisfied());
        assertEquals(.35,restored.multiplier);
    }
    @Test void networkBatchBoundsAreEnforced() {
        var id=java.util.UUID.randomUUID();
        var entry=new WeaponRequirementNetwork.Entry(ResourceLocation.parse("test:sword"),WeaponRequirementProfile.NONE);
        assertThrows(IllegalArgumentException.class,()->new WeaponRequirementNetwork.Batch(id,0,1,.35,java.util.Collections.nCopies(WeaponRequirementNetwork.BATCH_SIZE+1,entry)));
        assertThrows(IllegalArgumentException.class,()->new WeaponRequirementNetwork.Batch(id,0,WeaponRequirementNetwork.MAX_BATCHES+1,.35,java.util.List.of()));
        assertThrows(IllegalArgumentException.class,()->new WeaponRequirementNetwork.Batch(id,1,1,.35,java.util.List.of()));
        assertThrows(IllegalArgumentException.class,()->new WeaponRequirementNetwork.Batch(id,0,1,Double.NaN,java.util.List.of()));
    }
}
