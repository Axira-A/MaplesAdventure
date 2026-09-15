package dev.maplesadventure.progression;

import com.google.gson.JsonParser;
import dev.maplesadventure.progression.defense.*;
import dev.maplesadventure.progression.stats.DamageDefenseType;
import dev.maplesadventure.progression.weapon.*;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class EnemyDefenseTest {
    private static ResourceLocation id(String path) { return ResourceLocation.parse("test:" + path); }
    private static EntityDefenseProfile profile(String channels) {
        return EntityDefenseRegistry.parseProfile(id("enemy"), JsonParser.parseString("{\"channels\":" + channels + "}").getAsJsonObject());
    }
    private static WeaponDamageBundle single(WeaponDamageChannel channel, double ar, double requirement) {
        return new WeaponDamageBundle(Map.of(channel, new WeaponDamageBundle.ChannelAttack(ar, 0)), ar, 1, requirement);
    }
    private static WeaponDamageResolution resolve(double damage, WeaponDamageBundle bundle, EntityDefenseProfile profile) {
        return WeaponCombatResolutionService.resolve(damage, bundle, profile, .35);
    }
    @Test void canonicalMappingIsBijectiveAndPhysicalIsNotSlash() {
        assertEquals(9, WeaponDamageChannel.values().length);
        for (var type : DamageDefenseType.values()) assertEquals(type, DamageChannelMapping.preview(type.channel()));
        var enemy = profile("{\"physical\":{\"defense\":10000,\"absorption\":0.8},\"slash\":{\"defense\":0}}");
        for (var channel : List.of(WeaponDamageChannel.SLASH,WeaponDamageChannel.STRIKE,WeaponDamageChannel.PIERCE))
            assertEquals(25, resolve(25,single(channel,40,1),enemy).finalDamage());
        assertTrue(resolve(25,single(WeaponDamageChannel.PHYSICAL,40,1),enemy).finalDamage()<25);
    }
    @Test void curveMatchesReferenceAndRemainsContinuousFiniteAndBounded() {
        assertEquals(1,DefenseMitigationCurve.penetration(40,0));
        assertEquals(40.0/54,DefenseMitigationCurve.penetration(40,40));
        assertEquals(40.0/68,DefenseMitigationCurve.penetration(40,80));
        assertEquals(80.0/94,DefenseMitigationCurve.penetration(80,40));
        assertEquals(1,DefenseMitigationCurve.penetration(0,0));
        assertEquals(.1,DefenseMitigationCurve.penetration(0,10000));
        for (double ar : new double[]{0,.001,1,40,80,60000}) for(double defense : new double[]{0,1,40,80,10000})
            for(double absorption:new double[]{-.5,-.25,0,.15,.8}) {
                double factor=DefenseMitigationCurve.finalChannelMultiplier(ar,new ChannelDefense(defense,absorption),.35);
                assertTrue(Double.isFinite(factor)&&factor>=.05&&factor<=1.5);
            }
        assertEquals(.05,DefenseMitigationCurve.finalChannelMultiplier(1,new ChannelDefense(10000,.8),.35));
        assertEquals(1.25,DefenseMitigationCurve.finalChannelMultiplier(40,new ChannelDefense(0,-.25),.35));
        assertEquals(.8,DefenseMitigationCurve.finalChannelMultiplier(40,new ChannelDefense(0,.2),.35));
    }
    @Test void allInfusionsAndDefaultArchetypesAreBitExactRound9AtZeroDefense() {
        var scaling=new WeaponScalingProfile(.8,.4,0,0,0,1.15,"TEST","SWORD","");
        for(var archetype:WeaponRequirementArchetype.values()) for(var infusion:WeaponInfusionRegistry.definitions().values()) {
            var base=WeaponDamageProfile.automatic(archetype); var applied=infusion.apply(scaling,base);
            for(int stat:new int[]{5,20,39,40,60,99}) {
                var attributes=PlayerAttributeState.defaultsState();
                for(var attr:WeaponRequirementProfile.ATTRIBUTES) attributes=attributes.with(attr,stat,99);
                for(double penalty:new double[]{1,.35}) {
                    var bundle=WeaponAttackRatingCalculator.calculate(7.3,applied.scaling(),applied.damage(),attributes,penalty);
                    for(float incoming:new float[]{0,.1f,1.234567f,20.3f,1000.123f}) {
                        double old=incoming*bundle.effectiveMultiplier();
                        var result=resolve(incoming,bundle,EntityDefenseProfile.NONE);
                        assertEquals(Double.doubleToLongBits(old),Double.doubleToLongBits(result.finalDamage()));
                        assertEquals(Float.floatToIntBits((float)old),Float.floatToIntBits((float)result.finalDamage()));
                    }
                }
            }
        }
    }
    @Test void channelsSplitByFinalArNotBaseRatio() {
        var bundle=new WeaponDamageBundle(Map.of(WeaponDamageChannel.SLASH,new WeaponDamageBundle.ChannelAttack(75,75),
                WeaponDamageChannel.FIRE,new WeaponDamageBundle.ChannelAttack(50,0)),125,1.6,1);
        var result=resolve(20,bundle,profile("{\"slash\":{\"defense\":40},\"fire\":{\"absorption\":-0.25}}"));
        var slash=result.channelResults().stream().filter(c->c.channel()==WeaponDamageChannel.SLASH).findFirst().orElseThrow();
        assertEquals(.75,slash.share()); assertEquals(24,slash.incomingDamage());
        assertEquals(24*(150.0/164)+8*1.25,result.finalDamage(),1e-12);
    }
    @Test void unmetRequirementIsExactlyLastAfterMitigation() {
        var enemy=profile("{\"slash\":{\"defense\":10000,\"absorption\":0.8},\"holy\":{\"absorption\":-0.3}}");
        var channels=Map.of(WeaponDamageChannel.SLASH,new WeaponDamageBundle.ChannelAttack(65,20),
                WeaponDamageChannel.HOLY,new WeaponDamageBundle.ChannelAttack(35,10));
        var qualified=resolve(30,new WeaponDamageBundle(channels,100,1.3,1),enemy);
        var unmet=resolve(30,new WeaponDamageBundle(channels,100,1.3,.35),enemy);
        assertEquals(qualified.channelResults(),unmet.channelResults());
        assertEquals(qualified.finalDamage()*.35,unmet.finalDamage());
    }
    @Test void sacredWeaknessAndSplitDefenseTaxAreNatural() {
        var pure=single(WeaponDamageChannel.PHYSICAL,100,1);
        var split=new WeaponDamageBundle(Map.of(WeaponDamageChannel.PHYSICAL,new WeaponDamageBundle.ChannelAttack(65,0),
                WeaponDamageChannel.HOLY,new WeaponDamageBundle.ChannelAttack(35,0)),100,1,1);
        var undead=profile("{\"physical\":{\"defense\":30},\"holy\":{\"defense\":5,\"absorption\":-0.3}}");
        assertTrue(resolve(20,split,undead).finalDamage()>resolve(20,pure,undead).finalDamage());
        var equal=profile("{\"physical\":{\"defense\":40},\"holy\":{\"defense\":40}}");
        assertTrue(resolve(20,split,equal).finalDamage()<resolve(20,pure,equal).finalDamage());
    }
    @Test void rejectsMalformedAndUnsafeDataAsWholeProfiles() {
        for(String channels:List.of("{\"slash\":{\"defense\":-1}}","{\"slash\":{\"defense\":10001}}",
                "{\"slash\":{\"absorption\":0.81}}","{\"slash\":{\"absorption\":-0.51}}",
                "{\"fire\":{\"defense\":1e999}}","{\"fire\":{\"defense\":\"NaN\"}}",
                "{\"blood\":{\"defense\":2}}","{\"slash\":{\"armor\":3}}"))
            assertThrows(RuntimeException.class,()->profile(channels));
        assertThrows(IllegalArgumentException.class,()->new ChannelDefense(Double.NaN,0));
        assertThrows(IllegalArgumentException.class,()->new ChannelDefense(0,Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class,()->DefenseMitigationCurve.penetration(20,40,0));
    }
    @Test void exactWinsOverHigherPriorityTagAndTagOrderingIsStable() {
        var exact=new EntityDefenseRegistry.Rule(id("exact"),id("mob"),null,-100,id("a"));
        var tagZ=new EntityDefenseRegistry.Rule(id("z"),null,id("all"),100,id("b"));
        var tagA=new EntityDefenseRegistry.Rule(id("a"),null,id("all"),100,id("c"));
        var low=new EntityDefenseRegistry.Rule(id("low"),null,id("all"),0,id("d"));
        assertEquals(exact,EntityDefenseService.selectRule(id("mob"),t->true,List.of(tagZ,low,exact,tagA)).orElseThrow());
        assertEquals(tagA,EntityDefenseService.selectRule(id("other"),t->true,List.of(tagZ,low,exact,tagA)).orElseThrow());
        assertTrue(EntityDefenseService.selectRule(id("other"),t->false,List.of(tagZ,exact)).isEmpty());
        var rule=EntityDefenseRegistry.parseRule(id("parsed"),JsonParser.parseString("{\"entity\":\"minecraft:zombie\",\"profile\":\"test:enemy\"}").getAsJsonObject());
        assertEquals(ResourceLocation.parse("minecraft:zombie"),rule.entity());
        assertThrows(RuntimeException.class,()->EntityDefenseRegistry.parseRule(id("bad"),JsonParser.parseString("{\"tag\":\"test:x\",\"entity\":\"test:x\",\"profile\":\"test:x\"}").getAsJsonObject()));
    }
    @Test void overridePersistsAndDeletedOrFutureProfilesRemainUnknownIdentity() {
        var ref=new EntityDefenseProfileRef(id("override")); var restored=new EntityDefenseProfileRef();
        restored.deserializeNBT(null,ref.serializeNBT(null)); assertEquals(ref.profileId(),restored.profileId());
        var binding=new EntityDefenseService.Binding(id("type"),"EXACT");
        var p=profile("{\"slash\":{\"defense\":40}}");
        var value=EntityDefenseService.resolveReference(restored,binding,Map.of(id("override"),p));
        assertSame(p,value.profile()); assertEquals("EXPLICIT",value.source());
        var missing=EntityDefenseService.resolveReference(restored,binding,Map.of(id("type"),p));
        assertTrue(missing.unknown()); assertSame(EntityDefenseProfile.NONE,missing.profile()); assertEquals(id("override"),restored.profileId());
        var saved=ref.serializeNBT(null); saved.putInt("dataVersion",2); restored.deserializeNBT(null,saved);
        assertTrue(EntityDefenseService.resolveReference(restored,binding,Map.of(id("override"),p)).unknown());
    }
    @Test void projectileUsesFrozenArButFreshTargetDefense() {
        var bundle=new WeaponDamageBundle(Map.of(WeaponDamageChannel.PIERCE,new WeaponDamageBundle.ChannelAttack(65,13),
                WeaponDamageChannel.MAGIC,new WeaponDamageBundle.ChannelAttack(35,14)),100,1.27,.35);
        var snap=new ProjectileRequirementPenalty(new WeaponHitContext(id("bow"),bundle,
                new WeaponRequirementResult(false,Map.of(Attribute.DEXTERITY,1),.35,false),true,UUID.randomUUID()));
        var loaded=new ProjectileRequirementPenalty(); loaded.deserializeNBT(null,snap.serializeNBT(null));
        assertEquals(bundle,loaded.bundle());
        double old=resolve(20,loaded.bundle(),EntityDefenseProfile.NONE).finalDamage();
        double next=resolve(20,loaded.bundle(),profile("{\"magic\":{\"defense\":100,\"absorption\":0.5}}")).finalDamage();
        assertTrue(next<old); assertEquals(bundle,loaded.bundle());
    }
    @Test void zeroArHasNoNan() {
        var bundle=single(WeaponDamageChannel.SLASH,0,1);
        assertEquals(20,resolve(20,bundle,EntityDefenseProfile.NONE).finalDamage());
        assertTrue(Double.isFinite(resolve(20,bundle,profile("{\"slash\":{\"defense\":40}}")).finalDamage()));
    }
}
