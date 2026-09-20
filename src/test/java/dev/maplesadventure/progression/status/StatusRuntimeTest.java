package dev.maplesadventure.progression.status;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonParser;
import dev.maplesadventure.progression.defense.*;
import net.minecraft.resources.ResourceLocation;

class StatusRuntimeTest {
    private StatusRuntimeState state(StatusEffectType t) {
        var s=new StatusRuntimeState(); s.accumulate(t,40,StatusResistance.DEFAULT,StatusSourceContext.admin(t),100); return s;
    }
    @Test void decayWaitsThenStopsAtZero() {
        var s=state(StatusEffectType.BLEED); var e=s.get(StatusEffectType.BLEED); var d=StatusDefinitions.get(StatusEffectType.BLEED);
        e.decay(159,d); assertEquals(40,e.current);
        e.decay(180,d); assertEquals(35,e.current);
        e.decay(10000,d); assertEquals(0,e.current);
    }
    @Test void bleedHasNoDurationAndCanAccumulateAgain() {
        var s=state(StatusEffectType.BLEED);
        assertTrue(s.accumulate(StatusEffectType.BLEED,120,StatusResistance.DEFAULT,StatusSourceContext.admin(StatusEffectType.BLEED),101));
        assertFalse(s.get(StatusEffectType.BLEED).active(101));
    }
    @Test void durationDefinitionsRemainIndependent() {
        assertEquals(0,StatusDefinitions.get(StatusEffectType.BLEED).duration());
        assertEquals(.15,StatusDefinitions.get(StatusEffectType.BLEED).maxHealthFraction());
        assertEquals(.10,StatusDefinitions.get(StatusEffectType.FROSTBITE).maxHealthFraction());
        assertEquals(1.20,StatusDefinitions.get(StatusEffectType.FROSTBITE).damageTakenMultiplier());
        assertTrue(StatusDefinitions.get(StatusEffectType.SCARLET_ROT).damage(100,1)>StatusDefinitions.get(StatusEffectType.POISON).damage(100,1));
    }
    @Test void activeDoesNotStackButOtherStatusesCanAccumulate() {
        var s=state(StatusEffectType.POISON); var e=s.get(StatusEffectType.POISON); e.activeStart=100; e.activeEnd=1000; e.current=0;
        assertFalse(s.accumulate(StatusEffectType.POISON,200,StatusResistance.DEFAULT,StatusSourceContext.admin(StatusEffectType.POISON),200));
        assertEquals(0,e.current); assertEquals(1000,e.activeEnd);
        assertTrue(s.accumulate(StatusEffectType.SCARLET_ROT,200,StatusResistance.DEFAULT,StatusSourceContext.admin(StatusEffectType.SCARLET_ROT),200));
    }
    @Test void fullNbtRoundTripKeepsBuildupAndDuration() {
        var s=state(StatusEffectType.FROSTBITE); var e=s.get(StatusEffectType.FROSTBITE); e.activeStart=100; e.activeEnd=400; e.nextDot=120;
        var copy=new StatusRuntimeState(); copy.deserializeNBT(null,s.serializeNBT(null));
        assertEquals(s.serializeNBT(null),copy.serializeNBT(null)); assertTrue(copy.get(StatusEffectType.FROSTBITE).active(150));
    }
    @Test void invalidNbtIsContainedAndClearRemovesAll() {
        var s=state(StatusEffectType.BLEED); var n=s.serializeNBT(null); n.getList("entries",10).getCompound(0).putDouble("current",Double.NaN);
        var c=new StatusRuntimeState(); c.deserializeNBT(null,n); assertTrue(c.empty());
        s.clear(); assertTrue(s.empty());
    }
    @Test void oldDefenseProfilesAndNewResistanceUseSameResolver() {
        var id=ResourceLocation.parse("test:enemy");
        var old=EntityDefenseRegistry.parseProfile(id,JsonParser.parseString("{\"channels\":{\"slash\":{\"defense\":40}}}").getAsJsonObject());
        assertTrue(old.statusResistances().isEmpty());
        var next=EntityDefenseRegistry.parseProfile(id,JsonParser.parseString("{\"status_resistances\":{\"bleed\":{\"threshold\":200},\"scarlet_rot\":{\"immune\":true}}}").getAsJsonObject());
        assertEquals(200,next.statusResistances().get(StatusEffectType.BLEED).threshold());
        assertTrue(next.statusResistances().get(StatusEffectType.SCARLET_ROT).immune());
    }
    @Test void definitionsReloadParserIsBounded() {
        var json=JsonParser.parseString("{\"active_duration\":400,\"damage\":{\"max_health_fraction\":0.01}}").getAsJsonObject();
        assertEquals(400,StatusDefinitions.parse(StatusEffectType.POISON,json).duration());
        assertThrows(IllegalArgumentException.class,()->StatusDefinitions.parse(StatusEffectType.BLEED,json));
        assertThrows(IllegalArgumentException.class,()->new StatusResistance(Double.POSITIVE_INFINITY,false,1));
        assertThrows(IllegalArgumentException.class,()->new StatusResistance(0,false,1));
    }
}
