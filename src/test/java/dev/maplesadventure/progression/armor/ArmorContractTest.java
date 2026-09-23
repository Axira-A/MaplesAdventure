package dev.maplesadventure.progression.armor;

import com.google.gson.JsonParser;
import dev.maplesadventure.progression.PlayerAttributeState;
import dev.maplesadventure.progression.stats.DefenseCalculator;
import dev.maplesadventure.progression.stats.CharacterStatCalculator;
import dev.maplesadventure.progression.stats.CharacterStat;
import dev.maplesadventure.progression.weapon.*;
import dev.maplesadventure.progression.defense.*;
import dev.maplesadventure.progression.status.*;
import dev.maplesadventure.progression.weapon.WeaponDamageChannel;
import java.nio.file.*;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ArmorContractTest {
    private static ArmorProfileRules.Rule parse(String json) {
        return ArmorProfileRules.parse(ResourceLocation.parse("test:rule"),JsonParser.parseString(json).getAsJsonObject());
    }
    @Test void exampleAndDisabledRuleUsePublicGrammar() throws Exception {
        var path=Path.of("docs/integration/examples/datapack/data/example/maplesadventure/armor_profiles/holy_knight_chestplate.json");
        var rule=ArmorProfileRules.parse(ResourceLocation.parse("example:holy_knight_chestplate"),
                JsonParser.parseString(Files.readString(path)).getAsJsonObject());
        assertEquals(18,rule.profile().channel(WeaponDamageChannel.HOLY));
        assertEquals(12,rule.profile().resistance(StatusResistanceType.ROBUSTNESS));
        assertEquals(8,rule.profile().channel(WeaponDamageChannel.PHYSICAL));
        assertEquals(10,rule.profile().channel(WeaponDamageChannel.SLASH));
        assertTrue(parse("{\"item\":\"minecraft:diamond_chestplate\",\"disabled\":true}").disabled());
    }
    @Test void malformedRulesRejectIndependently() {
        for(var json:List.of(
                "{\"item\":\"minecraft:air\",\"tag\":\"minecraft:armor\",\"channels\":{\"holy\":1}}",
                "{\"channels\":{\"holy\":1}}", "{\"item\":\"minecraft:air\",\"wrong\":1}",
                "{\"item\":\"minecraft:air\",\"channels\":{\"holy\":-1}}",
                "{\"item\":\"minecraft:air\",\"channels\":{\"holy\":1001}}",
                "{\"item\":\"minecraft:air\",\"channels\":{\"holy\":\"NaN\"}}",
                "{\"item\":\"minecraft:air\",\"channels\":{\"unknown\":1}}",
                "{\"item\":\"minecraft:air\",\"resistances\":{\"unknown\":1}}"))
            assertThrows(RuntimeException.class,()->parse(json),json);
    }
    @Test void exactPriorityAndDisabledSuppressTag() {
        var item = ResourceLocation.parse("example:chest");
        var tag = ResourceLocation.parse("example:armor");
        var highTag = ArmorProfileRules.parse(ResourceLocation.parse("example:a_tag"),
                JsonParser.parseString("{\"tag\":\"example:armor\",\"priority\":100,\"channels\":{\"holy\":40}}").getAsJsonObject());
        var exact = ArmorProfileRules.parse(ResourceLocation.parse("example:z_exact"),
                JsonParser.parseString("{\"item\":\"example:chest\",\"channels\":{\"holy\":18}}").getAsJsonObject());
        assertEquals(exact, ArmorProfileService.select(List.of(highTag, exact), item, candidate -> candidate.equals(tag)).orElseThrow());
        var disabled = ArmorProfileRules.parse(ResourceLocation.parse("example:disabled"),
                JsonParser.parseString("{\"item\":\"example:chest\",\"disabled\":true}").getAsJsonObject());
        assertTrue(ArmorProfileService.select(List.of(highTag, disabled), item, candidate -> true).orElseThrow().disabled());
        var high = ArmorProfileRules.parse(ResourceLocation.parse("example:b"),
                JsonParser.parseString("{\"item\":\"example:chest\",\"priority\":10,\"channels\":{\"holy\":30}}").getAsJsonObject());
        var low = ArmorProfileRules.parse(ResourceLocation.parse("example:a"),
                JsonParser.parseString("{\"item\":\"example:chest\",\"priority\":0,\"channels\":{\"holy\":10}}").getAsJsonObject());
        assertEquals(high, ArmorProfileService.select(List.of(high, low), item, candidate -> false).orElseThrow());
        var lexicalA = ArmorProfileRules.parse(ResourceLocation.parse("example:a"),
                JsonParser.parseString("{\"item\":\"example:chest\",\"channels\":{\"holy\":1}}").getAsJsonObject());
        var lexicalB = ArmorProfileRules.parse(ResourceLocation.parse("example:b"),
                JsonParser.parseString("{\"item\":\"example:chest\",\"channels\":{\"holy\":2}}").getAsJsonObject());
        assertEquals(lexicalA, ArmorProfileService.select(List.of(lexicalA, lexicalB), item, candidate -> false).orElseThrow());
    }
    @Test void armorWireSnapshotIsBoundedAndRoundTrips() {
        var source=new ArmorEquipmentSnapshot(Map.of(WeaponDamageChannel.HOLY,18.,WeaponDamageChannel.PHYSICAL,8.),
                Map.of(StatusResistanceType.ROBUSTNESS,12.));
        var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),net.minecraft.core.RegistryAccess.EMPTY);
        source.write(buffer);
        assertEquals(13*Double.BYTES,buffer.readableBytes());
        assertEquals(source.channel(WeaponDamageChannel.HOLY),ArmorEquipmentSnapshot.read(buffer).channel(WeaponDamageChannel.HOLY));
        assertThrows(IllegalArgumentException.class,()->new ArmorEquipmentSnapshot(Map.of(WeaponDamageChannel.HOLY,Double.POSITIVE_INFINITY),Map.of()));
        assertThrows(IllegalArgumentException.class,()->new ArmorEquipmentSnapshot(Map.of(),Map.of(StatusResistanceType.FOCUS,-1.)));
        assertThrows(IllegalArgumentException.class,()->new dev.maplesadventure.api.armor.ArmorProfileView(
                ResourceLocation.parse("example:invalid"),
                Map.of(dev.maplesadventure.api.damage.MaplesDamageChannel.HOLY,1001.),Map.of()));
    }
    @Test void attributeWireAndLevelUpPreviewKeepArmorContribution() {
        var base=PlayerAttributeState.defaultsState();
        var armor=new ArmorEquipmentSnapshot(Map.of(WeaponDamageChannel.HOLY,18.),
                Map.of(StatusResistanceType.ROBUSTNESS,12.));
        var source=new dev.maplesadventure.progression.AttributeSnapshot(base,5,20,100,20,118,
                dev.maplesadventure.progression.runtime.RuntimeResourceSnapshot.progressionOnly(base),
                dev.maplesadventure.progression.encumbrance.EquipLoadRuntimeSnapshot.unavailable(),
                dev.maplesadventure.progression.spell.SpellSchoolScalingSnapshot.empty(),
                dev.maplesadventure.progression.weapon.WeaponLoadoutSnapshot.empty(),armor);
        var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),net.minecraft.core.RegistryAccess.EMPTY);
        dev.maplesadventure.progression.network.AttributePayloads.Snapshot.STREAM_CODEC.encode(buffer,
                new dev.maplesadventure.progression.network.AttributePayloads.Snapshot(source));
        var decoded=dev.maplesadventure.progression.network.AttributePayloads.Snapshot.STREAM_CODEC.decode(buffer).snapshot();
        assertEquals(18,decoded.armor().channel(WeaponDamageChannel.HOLY));
        assertEquals(12,decoded.armor().resistance(StatusResistanceType.ROBUSTNESS));
        assertEquals(18,decoded.characterStats().value(CharacterStat.HOLY_DEFENSE).breakdown().equipment());
        var preview=dev.maplesadventure.progression.LevelUpPreviewCalculator.calculate(decoded,
                Map.of(dev.maplesadventure.progression.Attribute.FAITH,1),1000,99,1.0);
        assertEquals(18,preview.characterStats().value(CharacterStat.HOLY_DEFENSE).breakdown().equipment());
        assertEquals(12,preview.characterStats().value(CharacterStat.ROBUSTNESS).breakdown().equipment());
    }
    @Test void zeroProfilePreservesOldDefenseAndFamilyMapping() {
        var attributes=PlayerAttributeState.defaultsState();
        var zero=ArmorEquipmentSnapshot.EMPTY;
        var armor=new ArmorEquipmentSnapshot(Map.of(WeaponDamageChannel.PHYSICAL,20.,WeaponDamageChannel.HOLY,18.),
                Map.of(StatusResistanceType.ROBUSTNESS,12.));
        var old=DefenseCalculator.snapshot(attributes);
        var with=DefenseCalculator.snapshot(attributes,armor);
        assertEquals(old.channel(WeaponDamageChannel.SLASH).defense(),with.channel(WeaponDamageChannel.SLASH).defense());
        assertEquals(old.channel(WeaponDamageChannel.PHYSICAL).defense()+20,with.channel(WeaponDamageChannel.PHYSICAL).defense());
        assertEquals(old.channel(WeaponDamageChannel.HOLY).defense()+18,with.channel(WeaponDamageChannel.HOLY).defense());
        assertEquals(old.channel(WeaponDamageChannel.HOLY).defense(),DefenseCalculator.snapshot(attributes,zero).channel(WeaponDamageChannel.HOLY).defense());
        var base=PlayerStatusResistanceCalculator.calculate(attributes);
        var improved=PlayerStatusResistanceCalculator.calculate(attributes,armor);
        assertEquals(base.robustness()+12,improved.robustness());
        assertEquals(base.immunity(),improved.immunity());
        assertEquals(base.focus(),improved.focus());
        assertEquals(base.vitality(),improved.vitality());
        assertEquals(StatusResistanceType.ROBUSTNESS,StatusEffectType.BLEED.resistanceType());
        assertEquals(StatusResistanceType.ROBUSTNESS,StatusEffectType.FROSTBITE.resistanceType());
    }
    @Test void armorDefenseChangesOnlyItsChannelWithinOneHit() {
        var attributes=PlayerAttributeState.defaultsState();
        var bare=DefenseCalculator.snapshot(attributes);
        var holyArmor=DefenseCalculator.snapshot(attributes,new ArmorEquipmentSnapshot(
                Map.of(WeaponDamageChannel.HOLY,18.),Map.of()));
        var holy=new WeaponDamageBundle(Map.of(WeaponDamageChannel.HOLY,
                new WeaponDamageBundle.ChannelAttack(20,0)),20,1,1);
        var physical=new WeaponDamageBundle(Map.of(WeaponDamageChannel.PHYSICAL,
                new WeaponDamageBundle.ChannelAttack(20,0)),20,1,1);
        var holyBare=WeaponCombatResolutionService.resolve(10,holy,bare,.35);
        var holyEquipped=WeaponCombatResolutionService.resolve(10,holy,holyArmor,.35);
        assertTrue(holyEquipped.finalDamage()<holyBare.finalDamage());
        assertEquals(1,holyEquipped.channelResults().size());
        assertEquals(WeaponCombatResolutionService.resolve(10,physical,bare,.35).finalDamage(),
                WeaponCombatResolutionService.resolve(10,physical,holyArmor,.35).finalDamage());
        var physicalArmor=DefenseCalculator.snapshot(attributes,new ArmorEquipmentSnapshot(
                Map.of(WeaponDamageChannel.PHYSICAL,20.),Map.of()));
        var slash=new WeaponDamageBundle(Map.of(WeaponDamageChannel.SLASH,
                new WeaponDamageBundle.ChannelAttack(20,0)),20,1,1);
        assertEquals(WeaponCombatResolutionService.resolve(10,slash,bare,.35).finalDamage(),
                WeaponCombatResolutionService.resolve(10,slash,physicalArmor,.35).finalDamage());
    }
    @Test void characterBreakdownAndDraftKeepEquipmentFixed() {
        var base=PlayerAttributeState.defaultsState();
        var armor=new ArmorEquipmentSnapshot(Map.of(WeaponDamageChannel.HOLY,18.),
                Map.of(StatusResistanceType.ROBUSTNESS,12.));
        var runtime=dev.maplesadventure.progression.runtime.RuntimeResourceSnapshot.progressionOnly(base);
        var load=dev.maplesadventure.progression.encumbrance.EquipLoadRuntimeSnapshot.unavailable();
        var schools=dev.maplesadventure.progression.spell.SpellSchoolScalingSnapshot.empty();
        var before=CharacterStatCalculator.calculate(base,runtime,load,schools,armor);
        var after=CharacterStatCalculator.calculate(base.with(dev.maplesadventure.progression.Attribute.FAITH,6,99),runtime,load,schools,armor);
        assertEquals(18,before.value(CharacterStat.HOLY_DEFENSE).breakdown().equipment());
        assertEquals(12,before.value(CharacterStat.ROBUSTNESS).breakdown().equipment());
        assertEquals(18,after.value(CharacterStat.HOLY_DEFENSE).breakdown().equipment());
        assertTrue(after.value(CharacterStat.HOLY_DEFENSE).breakdown().attribute()>
                before.value(CharacterStat.HOLY_DEFENSE).breakdown().attribute());
    }
}
