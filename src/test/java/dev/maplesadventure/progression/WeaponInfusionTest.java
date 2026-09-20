package dev.maplesadventure.progression;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.maplesadventure.progression.weapon.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class WeaponInfusionTest {
    private static final WeaponRequirementProfile REQUIREMENTS=new WeaponRequirementProfile(18,12,0,0,0,"TEST","GREATSWORD","");
    private static final WeaponScalingProfile SCALING=new WeaponScalingProfile(.75,.15,0,0,0,1.15,"TEST","GREATSWORD","");
    private static final WeaponDamageProfile DAMAGE=WeaponDamageProfile.automatic(WeaponRequirementArchetype.GREATSWORD);
    private static WeaponCombatProfileResolver.Resolved base() { return new WeaponCombatProfileResolver.Resolved(REQUIREMENTS,SCALING,DAMAGE,true,WeaponInfusionView.normal()); }
    private static WeaponInfusionEligibility all() { return new WeaponInfusionEligibility(WeaponInfusionRegistry.definitions().keySet()); }
    private static WeaponCombatProfileResolver.Resolved resolve(ResourceLocation id) {
        return WeaponCombatProfileResolver.resolveInfusion(base(),WeaponInfusionState.of(id),WeaponInfusionRegistry.definitions(),all());
    }
    private static PlayerAttributeState stats(int str,int dex,int intelligence,int faith,int arcane) {
        var state=PlayerAttributeState.defaultsState();
        return state.with(Attribute.STRENGTH,str,99).with(Attribute.DEXTERITY,dex,99).with(Attribute.INTELLIGENCE,intelligence,99)
                .with(Attribute.FAITH,faith,99).with(Attribute.ARCANE,arcane,99);
    }

    @Test void implicitAndExplicitNormalAreExactRound8Identity() {
        var implicit=WeaponCombatProfileResolver.resolveInfusion(base(),null,WeaponInfusionRegistry.definitions(),all());
        var explicit=resolve(WeaponInfusionRegistry.NORMAL_ID);
        assertSame(REQUIREMENTS,explicit.requirements()); assertEquals(SCALING,explicit.scaling()); assertEquals(DAMAGE,explicit.damage());
        assertEquals(implicit.requirements(),explicit.requirements()); assertEquals(implicit.scaling(),explicit.scaling()); assertEquals(implicit.damage(),explicit.damage());
        for(int value=5;value<=99;value++) {
            var state=stats(value,value,value,value,value);
            var round8=WeaponAttackRatingCalculator.calculate(20,SCALING,DAMAGE,state,.35);
            var round9=WeaponAttackRatingCalculator.calculate(20,explicit.scaling(),explicit.damage(),state,.35);
            assertEquals(round8,round9);
        }
    }

    @Test void physicalInfusionsUseCentralizedTransformsAndPreserveRequirements() {
        var heavy=resolve(WeaponInfusionRegistry.HEAVY_ID); var keen=resolve(WeaponInfusionRegistry.KEEN_ID);
        var quality=resolve(WeaponInfusionRegistry.QUALITY_ID);
        assertSame(REQUIREMENTS,heavy.requirements()); assertSame(REQUIREMENTS,keen.requirements()); assertSame(REQUIREMENTS,quality.requirements());
        assertEquals(.8,heavy.scaling().strength()); assertEquals(.0375,heavy.scaling().dexterity(),1e-12);
        assertEquals(.1875,keen.scaling().strength(),1e-12); assertEquals(.8,keen.scaling().dexterity());
        assertEquals(.65,quality.scaling().strength()); assertEquals(.55,quality.scaling().dexterity());
        assertEquals(.95,heavy.damage().components().getFirst().baseRatio());
        assertEquals(.95,keen.damage().components().getFirst().baseRatio());
        assertEquals(.94,quality.damage().components().getFirst().baseRatio());
        var str=stats(99,5,5,5,5); var dex=stats(5,99,5,5,5); var both=stats(99,99,5,5,5);
        assertTrue(ar(heavy,str)>ar(heavy,dex)); assertTrue(ar(keen,dex)>ar(keen,str));
        assertTrue(ar(quality,both)>ar(quality,str)); assertTrue(ar(quality,both)>ar(quality,dex));
    }

    @Test void magicAndSacredSplitExactlyAndOnlyTheirIntendedStatScalesElement() {
        var magic=resolve(WeaponInfusionRegistry.MAGIC_ID); var sacred=resolve(WeaponInfusionRegistry.SACRED_ID);
        assertEquals(List.of(WeaponDamageChannel.SLASH,WeaponDamageChannel.MAGIC),magic.damage().components().stream().map(WeaponDamageComponent::channel).toList());
        assertEquals(.65,magic.damage().components().get(0).baseRatio()); assertEquals(.35,magic.damage().components().get(1).baseRatio());
        assertEquals(1,magic.damage().components().stream().mapToDouble(WeaponDamageComponent::baseRatio).sum());
        var low=stats(20,20,20,20,20); var intelligence=stats(20,20,40,20,20); var faith=stats(20,20,20,40,20);
        var magicLow=bundle(magic,low); var magicInt=bundle(magic,intelligence); var magicFaith=bundle(magic,faith);
        assertTrue(magicInt.channels().get(WeaponDamageChannel.MAGIC).attackRating()>magicLow.channels().get(WeaponDamageChannel.MAGIC).attackRating());
        assertEquals(magicLow.channels().get(WeaponDamageChannel.MAGIC),magicFaith.channels().get(WeaponDamageChannel.MAGIC));
        var sacredLow=bundle(sacred,low); var sacredFaith=bundle(sacred,faith);
        assertTrue(sacredFaith.channels().get(WeaponDamageChannel.HOLY).attackRating()>sacredLow.channels().get(WeaponDamageChannel.HOLY).attackRating());
        assertEquals(.35,sacred.damage().components().get(1).baseRatio());
    }

    @Test void bloodAndPoisonAreArcanePhysicalOnlyAndDoNotInventChannels() {
        var blood=resolve(WeaponInfusionRegistry.BLOOD_ID); var poison=resolve(WeaponInfusionRegistry.POISON_ID);
        assertEquals(Set.of(WeaponDamageChannel.SLASH),bundle(blood,stats(20,20,99,99,99)).channels().keySet());
        assertEquals(dev.maplesadventure.progression.status.StatusEffectType.BLEED,blood.statuses().components().getFirst().type()); assertEquals(.55,blood.scaling().arcane());
        assertEquals(dev.maplesadventure.progression.status.StatusEffectType.POISON,poison.statuses().components().getFirst().type()); assertEquals(.45,poison.scaling().arcane());
        assertEquals(.9,blood.damage().components().getFirst().baseRatio()); assertEquals(.9,poison.damage().components().getFirst().baseRatio());
        assertTrue(ar(blood,stats(20,20,5,5,99))>ar(blood,stats(20,20,5,5,5)));
    }

    @Test void unknownAndIneligibleIdsPreserveBaseAndStoredIdentity() {
        var unknown=ResourceLocation.parse("test:removed");
        var missing=WeaponCombatProfileResolver.resolveInfusion(base(),WeaponInfusionState.of(unknown),WeaponInfusionRegistry.definitions(),all());
        assertEquals(WeaponInfusionView.Status.UNKNOWN,missing.infusion().status()); assertEquals(unknown,missing.infusion().id());
        assertEquals(SCALING,missing.scaling()); assertEquals(DAMAGE,missing.damage());
        var onlyNormal=new WeaponInfusionEligibility(Set.of(WeaponInfusionRegistry.NORMAL_ID));
        var rejected=WeaponCombatProfileResolver.resolveInfusion(base(),WeaponInfusionState.of(WeaponInfusionRegistry.HEAVY_ID),WeaponInfusionRegistry.definitions(),onlyNormal);
        assertEquals(WeaponInfusionView.Status.INELIGIBLE,rejected.infusion().status()); assertEquals(SCALING,rejected.scaling());
    }

    @Test void complexElementalBaseIsNotAutomaticallyInfusible() {
        var complex=new WeaponDamageProfile(List.of(WeaponDamageComponent.inherit(WeaponDamageChannel.SLASH),
                WeaponDamageComponent.inherit(WeaponDamageChannel.FIRE)),"TEST","BOSS","");
        assertFalse(WeaponInfusionEligibility.simplePhysical(complex));
        var disabled=WeaponInfusionEligibilityRules.parse(ResourceLocation.parse("test:no"),JsonParser.parseString(
                "{\"item\":\"minecraft:diamond_sword\",\"infusible\":false}").getAsJsonObject());
        assertFalse(disabled.infusible());
        var selected=WeaponInfusionEligibilityRules.parse(ResourceLocation.parse("test:yes"),JsonParser.parseString(
                "{\"tag\":\"test:weapons\",\"allowed\":[\"maplesadventure:heavy\",\"maplesadventure:keen\"]}").getAsJsonObject());
        assertEquals(2,selected.allowed().size());
        // Explicit eligibility has useful semantics: physical channels transform while pre-existing elemental channels remain intact.
        var heavy=WeaponInfusionRegistry.definitions().get(WeaponInfusionRegistry.HEAVY_ID).apply(SCALING,complex);
        assertEquals(.95,heavy.damage().components().get(0).baseRatio());
        assertEquals(WeaponDamageChannel.FIRE,heavy.damage().components().get(1).channel());
        assertEquals(1,heavy.damage().components().get(1).baseRatio());
        var magic=WeaponInfusionRegistry.definitions().get(WeaponInfusionRegistry.MAGIC_ID).apply(SCALING,complex);
        assertEquals(List.of(WeaponDamageChannel.SLASH,WeaponDamageChannel.FIRE,WeaponDamageChannel.MAGIC),
                magic.damage().components().stream().map(WeaponDamageComponent::channel).toList());
    }

    @Test void stateCodecWireAndItemCopiesPreserveDistinctPerStackValues() {
        var heavy=WeaponInfusionState.of(WeaponInfusionRegistry.HEAVY_ID); var keen=WeaponInfusionState.of(WeaponInfusionRegistry.KEEN_ID);
        var encoded=WeaponInfusionState.CODEC.encodeStart(JsonOps.INSTANCE,heavy).result().orElseThrow();
        assertEquals(heavy,WeaponInfusionState.CODEC.parse(JsonOps.INSTANCE,encoded).result().orElseThrow());
        var buffer=new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),RegistryAccess.EMPTY);
        try { WeaponInfusionState.STREAM_CODEC.encode(buffer,heavy); assertEquals(heavy,WeaponInfusionState.STREAM_CODEC.decode(buffer)); }
        finally { buffer.release(); }
        DataComponentType<WeaponInfusionState> type=DataComponentType.<WeaponInfusionState>builder().persistent(WeaponInfusionState.CODEC)
                .networkSynchronized(WeaponInfusionState.STREAM_CODEC).build();
        var a=new net.minecraft.core.component.PatchedDataComponentMap(net.minecraft.core.component.DataComponentMap.EMPTY);
        var b=new net.minecraft.core.component.PatchedDataComponentMap(net.minecraft.core.component.DataComponentMap.EMPTY);
        a.set(type,heavy); b.set(type,keen);
        assertNotEquals(a,b); assertEquals(heavy,a.copy().get(type)); assertEquals(keen,b.copy().get(type));
        var aCopy=a.copy(); b.set(type,heavy); assertEquals(heavy,aCopy.get(type));
    }

    @Test void datapackDefinitionOverrideAndWireRemainBounded() {
        var json=JsonParser.parseString("{\"display\":\"test.infusion\",\"base_multiplier\":0.91,\"physical_scaling\":{\"strength\":{\"minimum\":0.75}},\"future_buildup\":\"bleed\"}").getAsJsonObject();
        var changed=WeaponInfusionRegistry.parse(WeaponInfusionRegistry.HEAVY_ID,json,WeaponInfusionRegistry.definitions().get(WeaponInfusionRegistry.HEAVY_ID));
        assertEquals(.91,changed.baseMultiplier()); assertEquals(.75,changed.physicalScaling().get(Attribute.STRENGTH).minimum());
        assertEquals(WeaponInfusionBuildup.BLEED,changed.futureBuildup());
        var buffer=new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),RegistryAccess.EMPTY);
        try { changed.write(buffer); assertEquals(changed,WeaponInfusionDefinition.read(buffer)); all().write(buffer); assertEquals(all(),WeaponInfusionEligibility.read(buffer)); }
        finally { buffer.release(); }
    }

    @Test void projectileFreezesInfusedBundleAndRequirementPenaltyIsStillLast() {
        var magic=resolve(WeaponInfusionRegistry.MAGIC_ID); var attributes=stats(20,20,40,20,20);
        var initial=WeaponAttackRatingCalculator.calculate(20,magic.scaling(),magic.damage(),attributes,.35);
        var snapshot=new ProjectileRequirementPenalty(new WeaponHitContext(ResourceLocation.parse("test:weapon"),initial,
                new WeaponRequirementResult(false,Map.of(Attribute.STRENGTH,1),.35,false),false,UUID.randomUUID()));
        var restored=new ProjectileRequirementPenalty(); restored.deserializeNBT(null,snapshot.serializeNBT(null));
        resolve(WeaponInfusionRegistry.HEAVY_ID); WeaponAttackRatingCalculator.calculate(20,magic.scaling(),magic.damage(),stats(99,99,99,99,99),1);
        assertEquals(initial,restored.bundle()); assertEquals(initial.nominalMultiplier()*.35,restored.combatMultiplier(),1e-12);
        assertEquals(Set.of(WeaponDamageChannel.SLASH,WeaponDamageChannel.MAGIC),restored.bundle().channels().keySet());
    }

    private static double ar(WeaponCombatProfileResolver.Resolved profile,PlayerAttributeState state) { return bundle(profile,state).totalAttackRating(); }
    private static WeaponDamageBundle bundle(WeaponCombatProfileResolver.Resolved profile,PlayerAttributeState state) {
        return WeaponAttackRatingCalculator.calculate(20,profile.scaling(),profile.damage(),state,1);
    }
}
