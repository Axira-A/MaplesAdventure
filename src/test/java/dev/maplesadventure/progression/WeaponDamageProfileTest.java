package dev.maplesadventure.progression;

import com.google.gson.JsonParser;
import dev.maplesadventure.progression.weapon.*;
import dev.maplesadventure.progression.stats.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class WeaponDamageProfileTest {
    private PlayerAttributeState stats(int value) {
        var s=PlayerAttributeState.defaultsState(); for(var a:WeaponRequirementProfile.ATTRIBUTES) s=s.with(a,value,99); return s;
    }
    private WeaponDamageProfile parse(String components) {
        return WeaponDamageProfileRules.parse(ResourceLocation.parse("test:damage"),JsonParser.parseString("{\"item\":\"test:weapon\",\"components\":"+components+"}").getAsJsonObject()).profile();
    }
    private WeaponDamageProfile fire() { return parse("[{\"channel\":\"slash\",\"base_ratio\":0.75,\"scaling\":{\"strength\":0.35,\"dexterity\":0.25}},{\"channel\":\"fire\",\"base_ratio\":0.35,\"scaling\":{\"faith\":0.8,\"intelligence\":0.15}}]"); }
    @Test void everyDefaultArchetypeExactlyMatchesRound7() {
        for(var type:WeaponRequirementArchetype.values()) for(int value=5;value<=99;value++) for(double base:new double[]{.01,1,7,15,500,10000}) {
            var scaling=WeaponScalingProfile.automatic(type); var state=stats(value);
            var old=WeaponAttackRatingCalculator.calculate(base,scaling,state);
            var next=WeaponAttackRatingCalculator.calculate(base,scaling,WeaponDamageProfile.automatic(type),state,.35);
            assertEquals(old.damageMultiplier(),next.totalAttackRating()/base,1e-9,type+" "+value);
            assertEquals(old.attackRating(),next.totalAttackRating(),1e-9);
            assertEquals(old.damageMultiplier()*.35,next.effectiveMultiplier(),1e-9);
        }
    }
    @Test void physicalTypesDoNotInferElements() {
        assertEquals(WeaponDamageChannel.PHYSICAL,WeaponDamageChannel.automatic(WeaponRequirementArchetype.SWORD));
        for(var type:List.of(WeaponRequirementArchetype.GREATSWORD,WeaponRequirementArchetype.TACHI,WeaponRequirementArchetype.UCHIGATANA)) assertEquals(WeaponDamageChannel.SLASH,WeaponDamageChannel.automatic(type));
        for(var type:List.of(WeaponRequirementArchetype.DAGGER,WeaponRequirementArchetype.SPEAR,WeaponRequirementArchetype.BOW,WeaponRequirementArchetype.CROSSBOW,WeaponRequirementArchetype.TRIDENT)) assertEquals(WeaponDamageChannel.PIERCE,WeaponDamageChannel.automatic(type));
        assertEquals(WeaponDamageChannel.STRIKE,WeaponDamageChannel.automatic(WeaponRequirementArchetype.MACE));
    }
    @Test void fireFaithDoesNotChangeSlashAndPenaltyIsLast() {
        var profile=fire(); var s=stats(20); var high=s.with(Attribute.FAITH,40,99);
        var old=WeaponAttackRatingCalculator.calculate(100,WeaponScalingProfile.NONE,profile,s,1);
        var next=WeaponAttackRatingCalculator.calculate(100,WeaponScalingProfile.NONE,profile,high,.35);
        assertEquals(old.channels().get(WeaponDamageChannel.SLASH),next.channels().get(WeaponDamageChannel.SLASH));
        assertTrue(next.channels().get(WeaponDamageChannel.FIRE).attackRating()>old.channels().get(WeaponDamageChannel.FIRE).attackRating());
        assertEquals(110,next.totalBase(),1e-9);
        assertEquals(next.totalAttackRating()/100*.35,next.effectiveMultiplier(),1e-9);
        assertEquals(1,next.fraction(WeaponDamageChannel.FIRE)+next.fraction(WeaponDamageChannel.SLASH),1e-9);
    }
    @Test void explicitEmptyScalingIsNotInheritanceAndPureHolyWorks() {
        var damage=parse("[{\"channel\":\"physical\",\"base_ratio\":0.5},{\"channel\":\"magic\",\"base_ratio\":0.5,\"scaling\":{}}]");
        var r=WeaponAttackRatingCalculator.calculate(10,WeaponScalingProfile.automatic(WeaponRequirementArchetype.SWORD),damage,stats(99),1);
        assertEquals(0,r.channels().get(WeaponDamageChannel.MAGIC).scalingBonus());
        assertEquals(3.5,r.channels().get(WeaponDamageChannel.PHYSICAL).scalingBonus(),1e-9);
        var holy=parse("[{\"channel\":\"holy\",\"base_ratio\":1,\"scaling\":{\"faith\":0.8}}]");
        assertEquals(18,WeaponAttackRatingCalculator.calculate(10,WeaponScalingProfile.NONE,holy,stats(99),1).totalAttackRating(),1e-9);
    }
    @Test void snapshotRoundTripFrozenAndLegacyPreservesMultiplier() {
        var r=WeaponAttackRatingCalculator.calculate(15,WeaponScalingProfile.NONE,fire(),stats(40),.35);
        var context=new WeaponHitContext(ResourceLocation.parse("test:flame"),r,new WeaponRequirementResult(false,Map.of(),.35,false),false,UUID.randomUUID());
        var launched=new ProjectileRequirementPenalty(context); var copy=new ProjectileRequirementPenalty();
        copy.deserializeNBT(null,launched.serializeNBT(null));
        assertEquals(r,copy.bundle()); assertEquals(context.owner(),copy.context().owner()); assertTrue(copy.context().projectileSnapshot());
        assertEquals(context.usedWeapon(),copy.context().usedWeapon());
        WeaponAttackRatingCalculator.calculate(15,WeaponScalingProfile.NONE,fire(),stats(99),1);
        assertEquals(r,copy.bundle());
        var legacy=new CompoundTag(); legacy.putDouble("multiplier",.35); legacy.putDouble("scalingMultiplier",1.8); legacy.putInt("dataVersion",2);
        copy.deserializeNBT(null,legacy); assertEquals(.63,copy.combatMultiplier(),1e-9);
        assertEquals(Set.of(WeaponDamageChannel.PHYSICAL),copy.bundle().channels().keySet());
        legacy.remove("scalingMultiplier"); copy.deserializeNBT(null,legacy); assertEquals(.35,copy.combatMultiplier(),1e-9);
    }
    @Test void zeroBaseAndMaximumBoundsAreFinite() {
        var damage=parse("[{\"channel\":\"holy\",\"base_ratio\":2,\"scaling\":{\"faith\":1.5,\"arcane\":1.5},\"max_bonus\":2}]");
        var max=WeaponAttackRatingCalculator.calculate(10000,WeaponScalingProfile.NONE,damage,stats(99),1);
        assertEquals(60000,max.totalAttackRating()); assertEquals(6,max.nominalMultiplier());
        var zero=WeaponAttackRatingCalculator.calculate(0,WeaponScalingProfile.NONE,damage,stats(99),1);
        assertEquals(0,zero.totalAttackRating()); assertEquals(6,zero.nominalMultiplier());
    }
    @Test void invalidJsonIsRejected() {
        for(String c:List.of("[]","[{\"channel\":\"unknown\",\"base_ratio\":1}]","[{\"channel\":\"fire\",\"base_ratio\":0}]",
                "[{\"channel\":\"fire\",\"base_ratio\":1},{\"channel\":\"fire\",\"base_ratio\":1}]",
                "[{\"channel\":\"fire\",\"base_ratio\":1.5},{\"channel\":\"holy\",\"base_ratio\":1}]")) assertThrows(RuntimeException.class,()->parse(c));
        for(String value:List.of("-1","2.1","1e309","\"1\"")) assertThrows(RuntimeException.class,()->parse("[{\"channel\":\"fire\",\"base_ratio\":"+value+"}]"));
        assertThrows(RuntimeException.class,()->parse("[{\"channel\":\"fire\",\"base_ratio\":1,\"max_bonus\":1}]"));
    }
    @Test void characterMirrorAndDraftUseSameChannelBundle() {
        var base=stats(20); var after=base.with(Attribute.FAITH,40,99);
        var held=new WeaponLoadoutSnapshot.Held(ResourceLocation.parse("test:flame"),WeaponRequirementProfile.NONE,WeaponScalingProfile.NONE,100,true,fire());
        var loadout=new WeaponLoadoutSnapshot(held,WeaponLoadoutSnapshot.empty().offHand(),.35);
        var before=loadout.evaluate(base).getFirst().attack(); var view=loadout.evaluate(after).getFirst();
        assertEquals(before.bundle().channels().get(WeaponDamageChannel.SLASH),view.attack().bundle().channels().get(WeaponDamageChannel.SLASH));
        var stats=new CharacterStatsSnapshot(5,Map.of()).withWeapons(List.of(view));
        assertEquals(StatImplementationState.ACTIVE,stats.value(CharacterStat.MAIN_HAND_ATTACK).implementation());
        assertEquals(20,base.get(Attribute.FAITH));
    }
    @Test void profileWireRoundTripAndInvalidEnum() {
        var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),net.minecraft.core.RegistryAccess.EMPTY);
        try {
            fire().write(buffer); assertEquals(fire(),WeaponDamageProfile.read(buffer));
            buffer.clear(); buffer.writeVarInt(1); buffer.writeVarInt(999);
            assertThrows(IllegalArgumentException.class,()->WeaponDamageProfile.read(buffer));
            buffer.clear(); buffer.writeVarInt(9);
            assertThrows(IllegalArgumentException.class,()->WeaponDamageProfile.read(buffer));
        } finally { buffer.release(); }
    }
    @Test void fractionalRatioAndCorruptNewSnapshotAreSafe() {
        var profile=parse("[{\"channel\":\"ice\",\"base_ratio\":0.2,\"scaling\":{}}]");
        var bundle=WeaponAttackRatingCalculator.calculate(7,WeaponScalingProfile.NONE,profile,stats(99),.35);
        var snapshot=new ProjectileRequirementPenalty(new WeaponHitContext(ResourceLocation.parse("test:ice"),bundle,
                new WeaponRequirementResult(false,Map.of(),.35,false),false,UUID.randomUUID()));
        var restored=new ProjectileRequirementPenalty(); var tag=snapshot.serializeNBT(null); restored.deserializeNBT(null,tag);
        assertEquals(.07,restored.combatMultiplier(),1e-9);
        tag.getList("channels",net.minecraft.nbt.Tag.TAG_COMPOUND).getCompound(0).putDouble("bonus",Double.POSITIVE_INFINITY);
        restored.deserializeNBT(null,tag); assertEquals(.35,restored.combatMultiplier(),1e-9);
    }
    @Test void largestLegalMetadataFitsRegistryBatch() {
        String source="界".repeat(32),type="界".repeat(64),reason="界".repeat(512);
        var scaling=new WeaponScalingProfile(1.5,1.5,1.5,1.5,1.5,2,source,type,reason);
        var components=new ArrayList<WeaponDamageComponent>();
        for(int i=0;i<8;i++) components.add(new WeaponDamageComponent(WeaponDamageChannel.values()[i],.25,WeaponDamageComponent.ScalingMode.OVERRIDE,scaling));
        var damage=new WeaponDamageProfile(components,source,type,reason);
        var held=new WeaponLoadoutSnapshot.Held(ResourceLocation.parse("test:"+"a".repeat(251)),
                new WeaponRequirementProfile(99,99,99,99,99,source,type,reason),scaling,10000,true,damage);
        var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),net.minecraft.core.RegistryAccess.EMPTY);
        try {
            for(int i=0;i<WeaponRequirementNetwork.BATCH_SIZE;i++) WeaponLoadoutSnapshot.writeHeld(buffer,held);
            assertTrue(buffer.writerIndex()+64<WeaponRequirementNetwork.MAX_BYTES);
            assertEquals(held,WeaponLoadoutSnapshot.readHeld(buffer));
        } finally { buffer.release(); }
    }
}
